import http from "./http";

export interface AgentSaveRequest {
  agentName: string;
  description?: string;
  systemPrompt: string;
  modelName: string;
  ragEnable?: boolean;
  maxToken?: number;
  temperature?: number;
  topK?: number;
  knowledgeBaseIds?: number[];
}

export interface AgentResponse extends AgentSaveRequest {
  agentId: number;
  status: number;
  createdAt: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

export const agentApi = {
  create: (b: AgentSaveRequest) => http.post<never, number>("/api/agent", b),
  list: (p: { pageNum?: number; pageSize?: number; keyword?: string }) =>
    http.get<never, PageResult<AgentResponse>>("/api/agent/list", {
      params: p,
    }),
  detail: (id: number) => http.get<never, AgentResponse>(`/api/agent/${id}`),
  update: (id: number, b: AgentSaveRequest) =>
    http.put<never, void>(`/api/agent/${id}`, b),
  remove: (id: number) => http.delete<never, void>(`/api/agent/${id}`),
};
