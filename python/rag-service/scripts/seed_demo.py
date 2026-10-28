"""往 kb_1_demo 灌演示数据：MySQL(document/document_chunk) + Milvus 向量。

幂等：每次运行先清掉 kb_id=1 的旧文档/分片，再重新写入。
用法（在 python/rag-service 目录下）：
    .venv/Scripts/python.exe scripts/seed_demo.py
"""
import asyncio
import sys

import pymysql
from pymilvus import MilvusClient
from pymysql.cursors import DictCursor

sys.path.insert(0, ".")
from rag_service.clients.embedding import embed_texts  # noqa: E402
from rag_service.config import settings  # noqa: E402

TENANT_ID = 1
KB_ID = 1
COLLECTION = "kb_1_demo"
DOC_NAME = "房屋租赁规则说明.pdf"

CHUNKS = [
    "押金退还：租客在租期届满并完成房屋交接后，押金将在 7 个工作日内无息退还至原支付账户。",
    "提前退租：租期未满提前退租的，需提前 30 天书面通知，并按一个月租金标准支付违约金。",
    "租金支付：租金按月支付，每月 5 日前缴纳当月租金，逾期每日按应付金额的 0.05% 加收滞纳金。",
    "房屋维修：非租客原因导致的房屋主体结构、水电管线损坏，由房东负责维修并承担费用。",
]


def mysql_write_conn():
    """写库用 loh_app（只读账号 loh_ro 不能写）。"""
    return pymysql.connect(
        host=settings.mysql_host, port=settings.mysql_port,
        user="loh_app", password=settings.mysql_password,
        database=settings.mysql_db, charset="utf8mb4", cursorclass=DictCursor,
    )


async def main():
    vecs = await embed_texts(CHUNKS)
    print(f"1. 向量化 {len(vecs)} 条, 维度 {len(vecs[0])}")

    client = MilvusClient(uri=settings.milvus_uri, token=settings.milvus_token)

    conn = mysql_write_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "SELECT id FROM document WHERE tenant_id=%s AND kb_id=%s AND doc_name=%s AND deleted=0",
                [TENANT_ID, KB_ID, DOC_NAME],
            )
            old = cur.fetchall()
            if old:
                doc_ids = [r["id"] for r in old]
                ph = ",".join(["%s"] * len(doc_ids))
                cur.execute(
                    f"SELECT milvus_pk FROM document_chunk WHERE document_id IN ({ph})", doc_ids
                )
                pks = [r["milvus_pk"] for r in cur.fetchall() if r["milvus_pk"]]
                if pks:
                    client.delete(COLLECTION, ids=pks)
                cur.execute(f"DELETE FROM document_chunk WHERE document_id IN ({ph})", doc_ids)
                cur.execute(f"DELETE FROM document WHERE id IN ({ph})", doc_ids)
                conn.commit()
                print(f"2. 清理旧数据: document={len(doc_ids)} chunk pk={len(pks)}")

            cur.execute(
                "INSERT INTO document (tenant_id, kb_id, doc_name, file_type, file_size, storage_key,"
                " chunk_count, parse_status, status) VALUES (%s,%s,%s,'pdf',10240,'demo/rule.pdf',%s,2,1)",
                [TENANT_ID, KB_ID, DOC_NAME, len(CHUNKS)],
            )
            doc_id = cur.lastrowid

            chunk_ids = []
            for i, content in enumerate(CHUNKS):
                cur.execute(
                    "INSERT INTO document_chunk (tenant_id, kb_id, document_id, chunk_index, content,"
                    " char_count, milvus_pk) VALUES (%s,%s,%s,%s,%s,%s,0)",
                    [TENANT_ID, KB_ID, doc_id, i, content, len(content)],
                )
                chunk_ids.append(cur.lastrowid)
            conn.commit()
        print(f"3. MySQL 写入: document_id={doc_id}, chunk_ids={chunk_ids}")
    finally:
        conn.close()

    res = client.insert(COLLECTION, [
        {"vector": v, "chunk_id": cid, "kb_id": KB_ID, "tenant_id": TENANT_ID}
        for v, cid in zip(vecs, chunk_ids)
    ])
    pks = list(res["ids"])
    client.flush(COLLECTION)
    print(f"4. Milvus 写入: pks={pks}")

    conn = mysql_write_conn()
    try:
        with conn.cursor() as cur:
            for pk, cid in zip(pks, chunk_ids):
                cur.execute("UPDATE document_chunk SET milvus_pk=%s WHERE id=%s", [pk, cid])
        conn.commit()
    finally:
        conn.close()
    print(f"5. 回写 milvus_pk 完成")

    # 顺手验证过滤表达式与命中对象
    hits = client.search(
        COLLECTION, data=[vecs[0]], limit=3,
        filter=f"kb_id == {KB_ID} and tenant_id == {TENANT_ID}",
        output_fields=["chunk_id"], search_params={"metric_type": "COSINE"},
    )
    print("6. 过滤检索命中:", [(h.id, h["entity"]["chunk_id"], round(h.distance, 4)) for h in hits[0]])

    # 脏数据：只写 Milvus 不写 MySQL，验证 rag-service 会跳过
    dirty = client.insert(COLLECTION, [
        {"vector": vecs[0], "chunk_id": 999999, "kb_id": KB_ID, "tenant_id": TENANT_ID}
    ])
    client.flush(COLLECTION)
    print(f"7. 已插入脏数据（只存Milvus）: pk={list(dirty['ids'])[0]} chunk_id=999999")

    client.close()
    print("完成 ✅")


if __name__ == "__main__":
    asyncio.run(main())
