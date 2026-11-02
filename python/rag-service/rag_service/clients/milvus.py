from pymilvus import DataType, MilvusClient

from rag_service.config import settings

_client: MilvusClient | None = None


def ensure_collection(client: MilvusClient, name: str) -> None:
    """集合不存在则创建。

    放在 clients 层而不是 router 层：入库服务和消费者都要用，
    放 router 会导致 service 反向 import router，形成循环导入。
    """
    if client.has_collection(name):
        return
    schema = client.create_schema(auto_id=True, enable_dynamic_field=False)
    schema.add_field("pk", DataType.INT64, is_primary=True, auto_id=True)
    schema.add_field("vector", DataType.FLOAT_VECTOR, dim=settings.embed_dim)
    schema.add_field("chunk_id", DataType.INT64)
    schema.add_field("kb_id", DataType.INT64)
    schema.add_field("tenant_id", DataType.INT64)
    idx = client.prepare_index_params()
    idx.add_index(field_name="vector", index_type="AUTOINDEX", metric_type="COSINE")
    client.create_collection(name, schema=schema, index_params=idx)


def get_client() -> MilvusClient:
    """全局单例。

    MilvusClient 内部维护 gRPC 连接与连接池，每个请求 new 一个会把服务端连接数吃光。
    """
    global _client
    if _client is None:
        _client = MilvusClient(uri=settings.milvus_uri, token=settings.milvus_token)
    return _client


def close_client() -> None:
    global _client
    if _client is not None:
        _client.close()
        _client = None
