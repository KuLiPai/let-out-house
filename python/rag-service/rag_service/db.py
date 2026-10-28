import pymysql
from pymysql.cursors import DictCursor
from rag_service.config import settings

def _connect():
    return pymysql.connect(
        host=settings.mysql_host,
        port=settings.mysql_port,
        user=settings.mysql_user,
        password=settings.mysql_password,
        database=settings.mysql_db,
        charset="utf8mb4",
        cursorclass=DictCursor,
        connect_timeout=5,
        read_timeout=10
    )

def fetch_chunks(tenant_id:int,milvus_pks:list[int])->dict[int,dict]:
    """milvus_pk->分片行。tenant_id条件区分租户身份"""
    if not milvus_pks:
        return {}
    ph=",".join(["%s"]*len(milvus_pks))
    sql=(f"SELECT id,document_id,content,milvus_pk FROM document_chunk WHERE tenant_id=%s AND status=1 AND milvus_pk IN ({ph})")
    conn = _connect()
    try:
        with conn.cursor() as cur:
            cur.execute(sql,[tenant_id,*milvus_pks])
            return {row["milvus_pk"]:row for row in cur.fetchall()}
    finally:
        conn.close()

def fetch_kb_collections(tenant_id:int,kb_ids:list[int])->dict[int,dict]:
    """kb_id->collection_name   注意knowledge——base有逻辑删除，要带deleted=0"""
    if not kb_ids:
        return {}
    ph=",".join(["%s"]*len(kb_ids))
    sql=(f"SELECT id ,collection_name FROM knowledge_base WHERE tenant_id=%s AND status=1 AND deleted=0 AND id in ({ph})")
    conn=_connect()
    try:
        with conn.cursor() as cur:
            cur.execute(sql,[tenant_id,*kb_ids])
            return {row["id"]:row["collection_name"] for row in cur.fetchall()}
    finally:
        conn.close()