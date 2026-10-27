import asyncio
import json
import uuid

from fastapi import APIRouter, Header, FastAPI
from fastapi.responses import StreamingResponse

from app.schemas import ChatRequest

router=APIRouter(prefix="/api/agent/runtime",tags=["runtime"])

MOCK_REPLY="这是一段来自agent-runtime-api的模拟回答，用来验证SSE全链路是否打通"

async def _event_stream(req:ChatRequest,trace_id:str):
    full=[]
    try:
        for ch in MOCK_REPLY:
            full.append(ch)
            payload = {"type": "chunk", "content": ch, "traceId": trace_id}
            yield f"data: {json.dumps(payload, ensure_ascii=False)}\n\n"
            await asyncio.sleep(0.05)

        yield f"data: {json.dumps({'type': 'end','fullContent': ''.join(full),'tokenUsage': {'input': len(req.query), 'output': len(MOCK_REPLY)},'traceId': trace_id}, ensure_ascii=False)}\n\n"

    except Exception as e:
        yield f"data: {json.dumps({'type': 'error', 'content': str(e), 'traceId': trace_id}, ensure_ascii=False)}\n\n"


@router.post("/chat/stream")
async def chat_stream(
        req:ChatRequest,
        x_tenant_id:str|None=Header(default=None),
        x_trace_id:str|None=Header(default=None)
):
    trace_id=x_trace_id or uuid.uuid4().hex
    return StreamingResponse(
        _event_stream(req=req,trace_id=trace_id),
        media_type="text/event-stream",
        headers={
            "Cache-Control":"no-cache",
            "X-Accel-Buffering":"no",
            "Connection":"keep-alive"
        }
    )