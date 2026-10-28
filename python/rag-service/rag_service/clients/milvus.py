from pymilvus import MilvusClient

from rag_service.config import settings

_client: MilvusClient | None = None


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
