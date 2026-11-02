import asyncio

from rag_service.clients.embedding import embed_texts
from rag_service.clients.milvus import ensure_collection, get_client
from rag_service.config import settings
from rag_service.db import fetch_chunk_ids, fetch_doc_for_ingest
from rag_service.db_write import delete_chunks, insert_chunks, update_milvus_pks
from rag_service.ingest.parser import parse_file
from rag_service.ingest.splitter import split_text


class IngestError(Exception):
    """入库业务异常：带 code，便于 router 映射成统一响应体。"""

    def __init__(self, code: int, msg: str):
        super().__init__(msg)
        self.code = code
        self.msg = msg


async def ingest_document(doc_id: int, kb_id: int, tenant_id: int,
                          file_path: str, file_type: str) -> dict:
    """解析入库核心：解析 → 切片 → 向量化 → 写 Milvus + MySQL。

    供两处复用：同步接口 /api/rag/ingest、异步消费者（Redis Stream）。
    业务失败抛 IngestError，系统故障抛原始异常，由调用方决定如何记录。
    """
    # ① 校验文档归属
    doc = await asyncio.to_thread(fetch_doc_for_ingest, doc_id, tenant_id)
    if not doc or doc["kb_id"] != kb_id:
        raise IngestError(404, "文档不存在")

    # ② 解析 + 切片
    try:
        text = parse_file(file_path, file_type)
    except Exception as e:
        raise IngestError(400, f"解析失败：{str(e)[:150]}")
    chunks = split_text(text, settings.chunk_size, settings.chunk_overlap)
    if not chunks:
        raise IngestError(400, "未解析到文本内容")

    client = get_client()
    coll = doc["collection_name"]
    ensure_collection(client, coll)

    # ③ 清理旧分片（覆盖上传场景，幂等）
    old = await asyncio.to_thread(fetch_chunk_ids, doc_id, tenant_id)
    if old:
        ids = [r["id"] for r in old]
        try:
            client.delete(coll, filter=f"chunk_id in {ids}")
        except Exception as e:
            # 集合刚建或向量本来就没有，不影响后续入库
            print(f"[warn] 清理旧向量失败：{e}")
        await asyncio.to_thread(delete_chunks, doc_id, tenant_id)

    # ④ 分批向量化
    vecs: list[list[float]] = []
    for i in range(0, len(chunks), settings.embed_batch):
        vecs.extend(await embed_texts(chunks[i:i + settings.embed_batch]))
    if len(vecs) != len(chunks):
        raise IngestError(500, f"向量数量不匹配：{len(vecs)}!={len(chunks)}")

    # ⑤ 先写 MySQL 拿 chunk_id，再插 Milvus 带上 chunk_id
    rows = [{"tenant_id": tenant_id, "kb_id": kb_id, "document_id": doc_id, "chunk_index": i,
             "content": c, "char_count": len(c), "milvus_pk": 0}
            for i, c in enumerate(chunks)]
    await asyncio.to_thread(insert_chunks, rows)

    saved = await asyncio.to_thread(fetch_chunk_ids, doc_id, tenant_id)
    if len(saved) != len(chunks):
        raise IngestError(500, "分片落库数量不一致")

    res = client.insert(coll, [
        {"vector": v, "chunk_id": row["id"], "kb_id": kb_id, "tenant_id": tenant_id}
        for v, row in zip(vecs, saved)
    ])
    client.flush(coll)

    # ⑥ 回写 milvus_pk，检索回查正文靠它
    await asyncio.to_thread(update_milvus_pks, list(zip([r["id"] for r in saved], res["ids"])))
    return {"docId": doc_id, "chunkCount": len(chunks)}
