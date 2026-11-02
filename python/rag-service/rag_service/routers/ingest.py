import asyncio
import uuid

from fastapi import APIRouter, Header

from rag_service.clients.milvus import get_client
from rag_service.db import fetch_chunk_ids, fetch_doc_for_ingest
from rag_service.db_write import delete_chunks
from rag_service.schemas import IngestRequest
from rag_service.service.ingest_service import IngestError, ingest_document

router = APIRouter(prefix="/api/rag", tags=["rag-ingest"])


def _ok(data, trace_id):
    return {"code": 0, "msg": "ok", "data": data, "traceId": trace_id}


def _fail(code, msg, trace_id):
    return {"code": code, "msg": msg, "data": None, "traceId": trace_id}


@router.post("/ingest")
async def ingest(req: IngestRequest,
                 x_tenant_id: str | None = Header(default=None),
                 x_trace_id: str | None = Header(default=None)):
    """同步入库（联调 / 补数据用）。异步链路走 Redis Stream，复用同一个 service。"""
    trace_id = x_trace_id or uuid.uuid4().hex
    if not x_tenant_id or not x_tenant_id.isdigit():
        return _fail(400, "缺少或非法的X-Tenant-Id", trace_id)
    tenant_id = int(x_tenant_id)
    if tenant_id != req.tenantId:
        return _fail(403, "租户不匹配", trace_id)

    try:
        data = await ingest_document(req.docId, req.kbId, tenant_id, req.filePath, req.fileType)
        return _ok(data, trace_id)
    except IngestError as e:
        return _fail(e.code, e.msg, trace_id)
    except Exception as e:
        return _fail(500, str(e)[:200], trace_id)


@router.delete("/document/{doc_id}")
async def delete_document(doc_id: int,
                          x_tenant_id: str | None = Header(default=None),
                          x_trace_id: str | None = Header(default=None)):
    trace_id = x_trace_id or uuid.uuid4().hex
    if not x_tenant_id or not x_tenant_id.isdigit():
        return _fail(400, "缺少或非法的X-Tenant-Id", trace_id)
    tenant_id = int(x_tenant_id)

    doc = await asyncio.to_thread(fetch_doc_for_ingest, doc_id, tenant_id)
    old = await asyncio.to_thread(fetch_chunk_ids, doc_id, tenant_id)
    if doc and old:
        client = get_client()
        try:
            client.delete(doc["collection_name"], filter=f"chunk_id in {[r['id'] for r in old]}")
            client.flush(doc["collection_name"])
        except Exception as e:
            print(f"[warn] 删除向量失败：{e}")
    await asyncio.to_thread(delete_chunks, doc_id, tenant_id)
    return _ok({"docId": doc_id, "deleted": len(old)}, trace_id)
