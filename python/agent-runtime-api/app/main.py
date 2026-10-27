from fastapi import FastAPI
from app.routers import chat

app=FastAPI(title="agent-runtime-api",version="0.1.0")
app.include_router(chat.router)

@app.get("/health")
async def health():
    return {"status":"UP","service":"agent-runtime-api"}