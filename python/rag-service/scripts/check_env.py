"""rag-service 环境自检：配置 → MySQL / Milvus / Embedding 三段连通性一次跑完。

用法（在 python/rag-service 目录下）：
    .venv/Scripts/python.exe scripts/check_env.py
"""
import sys

import httpx
import pymysql
from pymilvus import MilvusClient

sys.path.insert(0, ".")
from rag_service.config import settings  # noqa: E402

FAIL = []


def check(name, fn):
    try:
        detail = fn()
        print(f"[OK]   {name}: {detail}")
    except Exception as e:
        FAIL.append(name)
        print(f"[FAIL] {name}: {str(e)[:160]}")


def check_mysql():
    conn = pymysql.connect(
        host=settings.mysql_host, port=settings.mysql_port,
        user=settings.mysql_user, password=settings.mysql_password,
        database=settings.mysql_db, charset="utf8mb4",
    )
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM document")
            docs = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM document_chunk")
            chunks = cur.fetchone()[0]
            cur.execute("SELECT kb_code, embedding_model, collection_name FROM knowledge_base")
            kbs = cur.fetchall()
        return f"document={docs} chunk={chunks} kb={kbs}"
    finally:
        conn.close()


def check_milvus():
    c = MilvusClient(uri=settings.milvus_uri, token=settings.milvus_token)
    try:
        version = c.get_server_version()
        colls = c.list_collections()
        dim = None
        if "kb_1_demo" in colls:
            desc = c.describe_collection("kb_1_demo")
            for f in desc["fields"]:
                if f["name"] == "vector":
                    dim = f.get("params", {}).get("dim")
        return f"version={version} collections={colls} kb_1_demo.dim={dim}"
    finally:
        c.close()


def check_embedding():
    r = httpx.post(
        f"{settings.embed_base_url}/embeddings",
        headers={"Authorization": f"Bearer {settings.embed_api_key}"},
        json={"model": settings.embed_model, "input": ["连通性自检"], "encoding_format": "float"},
        timeout=settings.embed_timeout,
    )
    r.raise_for_status()
    vec = r.json()["data"][0]["embedding"]
    if len(vec) != settings.embed_dim:
        raise ValueError(f"维度不符：实际 {len(vec)}，配置 {settings.embed_dim}")
    return f"model={settings.embed_model} dim={len(vec)}"


if __name__ == "__main__":
    print(f"配置来源: LOH_* 环境变量 / .env")
    print(f"Milvus : {settings.milvus_uri} (auth={'on' if settings.milvus_token else 'off'})")
    print(f"Embed  : {settings.embed_base_url} / {settings.embed_model}")
    print(f"MySQL  : {settings.mysql_user}@{settings.mysql_host}/{settings.mysql_db}")
    print("-" * 60)
    check("MySQL 只读连接", check_mysql)
    check("Milvus 连接与集合", check_milvus)
    check("Embedding 接口", check_embedding)
    print("-" * 60)
    print("全部通过 ✅" if not FAIL else f"失败项：{FAIL}")
    sys.exit(1 if FAIL else 0)
