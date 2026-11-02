import asyncio
import os
import socket
import uuid

import redis
from redis import asyncio as aioredis
from redis.exceptions import ResponseError

from rag_service.config import settings
from rag_service.db_write import update_parse_status
from rag_service.service.ingest_service import IngestError,ingest_document

CONSUMER_NAME=f"{socket.gethostname()}-{os.getpid()}"
STATUS_RUNNING,STATUS_DONE,STATUS_FAILED=1,2,3

def _new_redis()->aioredis.Redis:
    return aioredis.Redis(
        host=settings.redis_host,
        port=settings.redis_port,
        password=settings.redis_password,
        db=settings.redis_db,
        decode_responses=True,    #bytes->string
        socket_connect_timeout=5,
        socket_timeout=None
    )


async def _ensure_group(r)->None:
    """消费组必须存在，否则XREADGROUP报NOGROUP"""
    try:
        await r.xgroup_create(settings.stream_key,settings.consumer_group,id="0",mkstream=True)
    except ResponseError as e:
        if "BUSYGROUP" not in str(e):
            raise

async def _handle(r,msg_id:str,f:dict)->None:
    doc_id,tenant_id=int(f["docId"]),int(f["tenantId"])
    kb_id=int(f["kbId"])
    try:
        data=await ingest_document(doc_id,kb_id,tenant_id,f["filePath"],f["fileType"])
        await asyncio.to_thread(update_parse_status,doc_id,tenant_id,STATUS_DONE,data["chunkCount"])
        await r.xack(settings.stream_key,settings.consumer_group,msg_id)
        print(f"[ingest] 完成docId={doc_id} chunk={data['chunkCount']} msg={msg_id}")
    except IngestError as e:
        # 文档被删了（404）属于无需重试，直接ack
        if e.code == 404:
            await r.xack(settings.stream_key,settings.consumer_group,msg_id)
            print(f"[ingest] 跳过（文档已不存在）")
            return
        await _retry_or_dlq(r, msg_id, f, e.msg, doc_id, tenant_id)
    except Exception as e:
        await _retry_or_dlq(r, msg_id, f, str(e)[:200], doc_id, tenant_id)

async def _retry_or_dlq(r,msg_id,f,reason,doc_id,tenant_id):
    key=f"loh:retry:{settings.stream_key}:{msg_id}"
    n=await r.hincrby(key,"n",1)
    await r.expire(key,86400)
    if n <settings.consumer_max_retry:
        print(f"[ingest] 第{n}次失败，留待重试 docId={doc_id}:{reason}")
        return
    await r.xadd(settings.dlq_key,{**f,"error":reason,"srcMsgId":msg_id})
    await asyncio.to_thread(update_parse_status,doc_id,tenant_id,STATUS_FAILED,0,reason[:500])
    await r.xack(settings.stream_key,settings.consumer_group,msg_id)
    print(f"[ingest]转入死信交换机 docId={doc_id}:{reason}")

async def _claim_stale(r)->None:
    """认领idle超时的pending消息（消费者宕机后，消息卡在别人名下）"""
    try:
        resp=await r.xautoclaim(
            settings.stream_key,
            settings.consumer_group,
            CONSUMER_NAME,
            min_idle_time=settings.consumer_claim_idle_ms,
            start_id="0-0",
            count=10
        )
        for msg_id,f in resp[1]:
            await _handle(r,msg_id,f)
    except ResponseError as e:
        print(f"[warn] XAUTOCLAIM 跳过：{e}")

async def run_forever()->None:
    r=_new_redis()
    await _ensure_group(r)
    print(f"[consumer]已启动name={CONSUMER_NAME} stream={settings.stream_key}")
    last_claim=asyncio.get_event_loop().time()
    while True:
        try:
            #每三十秒做一次僵尸消息认领
            now=asyncio.get_event_loop().time()
            if now-last_claim>30:
                await _claim_stale(r)
                last_claim=now

            resp=await r.xreadgroup(
                settings.consumer_group,CONSUMER_NAME,
                {settings.stream_key:">"},
                count=1,block=settings.consumer_block_ms,
            )
            for _stream,entries in resp or []:
                for msg_id,fields in entries:
                    await _handle(r,msg_id,fields)
        except asyncio.CancelledError as e:
            print("[consumer]收到取消信号，退出")
            await r.aclose()
            raise
        except Exception as e:
            print(f"[consumer]循环异常，1秒后重试：{e}")
            await asyncio.sleep(1)

        
