from pydantic import BaseModel

class ChatRequest(BaseModel):
    conversationId:int
    agentId:int
    query:str
    stream:bool=True