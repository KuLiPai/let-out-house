import asyncio
import httpx
from rag_service.config import settings

async def embed_texts(texts:list[str])->list[list[float]]:
    """批量向量化，返回顺序与入参一致"""
    if not texts:
        return []
    if not settings.embed_api_key:
        raise RuntimeError("未配置LOH_EMBED_API_KEY")
    url=f"{settings.embed_base_url}/embeddings"
    headers={"Authorization":f"Bearer {settings.embed_api_key}"}
    payload={"model":settings.embed_model,"input":texts,"encoding_format":"float"}
    last_err=""
    async with httpx.AsyncClient(timeout=settings.embed_timeout) as client:
        for attempt in range(3):
            r=await client.post(url=url,json=payload,headers=headers)
            if r.status_code ==200:
                data=sorted(r.json()["data"],key=lambda d:d["index"])
                vecs=[d["embedding"]for d in data]
                for v in vecs:
                    if len(v)!=settings.embed_dim:
                        raise ValueError(f"向量维度不符：{len(v)}!={settings.embed_dim}")
                return vecs
            if r.status_code in (429,500,502,503,504):
                last_err=f"{r.status_code}{r.text[:200]}"
                await asyncio.sleep(2**attempt)
                continue
            raise RuntimeError(f"embedding调用失败：{r.status_code}{r.text[:200]}")
    raise RuntimeError(f"embedding重试三次仍失败：{last_err}")