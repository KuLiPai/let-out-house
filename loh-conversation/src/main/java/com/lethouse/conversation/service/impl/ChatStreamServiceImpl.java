package com.lethouse.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lethouse.common.context.TenantContext;
import com.lethouse.common.exception.BusinessException;
import com.lethouse.conversation.dto.ChatStreamRequest;
import com.lethouse.conversation.entity.Conversation;
import com.lethouse.conversation.entity.Message;
import com.lethouse.conversation.mapper.ConversationMapper;
import com.lethouse.conversation.mapper.MessageMapper;
import com.lethouse.conversation.service.ChatStreamService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatStreamServiceImpl implements ChatStreamService {


    private static final Integer MAXWIDTH=20;


    private final HttpClient agentRuntimeClient;
    private final ConversationMapper conversationMapper;
    private final ObjectMapper objectMapper;
    private final MessageMapper messageMapper;

    @Value("${loh.agent-runtime.base-url}")
    private String baseUrl;

    @Override
    public void stream(Long conversationId, ChatStreamRequest request, HttpServletResponse response){
        //校验会话归属
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getId, conversationId)
                        .eq(Conversation::getUserId, TenantContext.getUserId())
        );
        if (conversation==null) {
            throw new BusinessException(404,"会话不存在");
        }

        //先调上游，拿到成功响应
        HttpResponse<InputStream> upstream;
        try {
            upstream=agentRuntimeClient.send(buildRequest(conversation,request),HttpResponse.BodyHandlers.ofInputStream());
        }catch (IOException e){
            Thread.currentThread().interrupt();
            throw  new BusinessException(503,"AI服务暂不可用");
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new BusinessException(503,"AI服务暂不可用");
        }
        if (upstream.statusCode()!=200) {
            try {
                upstream.body().close();
            } catch (IOException ignored) {
                //关闭失败不影响错误返回
            }
            throw  new BusinessException(502,"AI服务返回异常");
        }
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(request.getQuery());
        message.setMsgStatus(1);
        message.setTraceId(MDC.get("traceId"));
        messageMapper.insert(message);

        //设置SSE响应头
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control","no-cache");
        response.setHeader("X-Accel-Buffering","no");

        StringBuilder collected = new StringBuilder();
        String endContent=null;
        Integer tokenIn=null;
        Integer tokenOut=null;
        boolean interrupted=false;
        //搬运
        try(BufferedReader reader = new BufferedReader(
                new InputStreamReader(upstream.body(), StandardCharsets.UTF_8)
        )){
            ServletOutputStream out = response.getOutputStream();
            String line;
            while((line=reader.readLine())!=null){
                out.write(line.getBytes(StandardCharsets.UTF_8));
                out.write('\n');
                out.flush();
                if (line.startsWith("data:")) {
                    try {
                        JsonNode node = objectMapper.readTree(line.substring(5).trim());
                        String type = node.path("type").asText();
                        if ("chunk".equals(type)) {
                            collected.append(node.path("content").asText(""));
                        }else if("end".equals(type)){
                            endContent=node.path("fullContent").asText("");
                            tokenIn=node.path("tokenUsage").path("input").asInt();
                            tokenOut=node.path("tokenUsage").path("output").asInt();
                        }
                    }catch (Exception e){
                        log.debug("SSE时间解析跳过：{}",line);
                    }
                }
            }
        } catch (IOException e) {
            //客户端主动断开（关闭页面/切换路由）走到这里，正常现象
            interrupted=true;
            log.debug("SSE流被中断：conversationId={}",conversationId);
        }finally {
            persistTurn(conversationId,conversation,request.getQuery(),endContent!=null?endContent:collected.toString(),tokenIn,tokenOut,interrupted);
        }
    }

    private HttpRequest buildRequest(Conversation conversation,ChatStreamRequest request){
        Map<String, Object> body = Map.of("conversationId", conversation.getId(),
                "agentId", conversation.getAgentId(),
                "query", request.getQuery(),
                "stream", true
        );
        try {
            return HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl+"/api/agent/runtime/chat/stream"))
                    .header("Content-Type","application/json")
                    .header("X-Tenant-Id",String.valueOf(TenantContext.getTenantId()))
                    .header("X-Trace-Id", MDC.get("traceId"))
                    .header("X-Request-Id",TenantContext.getRequestId())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        }catch (JsonProcessingException e){
            throw  new BusinessException(500,"请求序列化失败");
        }
    }

    private void persistTurn(Long conversationId,Conversation conversation,String query,String answer,Integer tokenIn,Integer tokenOut,boolean interrupted){

        try {
            boolean hasAnswer = StringUtils.hasText(answer);

            if (hasAnswer) {
                Message message = new Message();
                message.setConversationId(conversationId);
                message.setRole("assistant");
                message.setContent(answer);
                message.setTokenInput(tokenIn);
                message.setTokenOutput(tokenOut);
                message.setMsgStatus(interrupted ? 0 : 1);
                message.setTraceId(MDC.get("traceId"));
                messageMapper.insert(message);
            }
            conversationMapper.update(null, new LambdaUpdateWrapper<Conversation>()
                    .eq(Conversation::getId, conversationId)
                    .setSql("message_count=message_count+" + (hasAnswer ? 2 : 1)+",last_message_at=CURRENT_TIMESTAMP(3)")
                    .set(Conversation::getLastMessageAt, LocalDateTime.now())
                    .set("新对话".equals(conversation.getTitle()), Conversation::getTitle, abbreviate(query, MAXWIDTH)));
        }catch (Exception e){
            log.error("对话落库失败：conversationId={}",conversationId,e);
        }
    }

    private String abbreviate(String text,int maxWidth){
        if (text==null||text.length()<=maxWidth) {
            return text;
        }
        return text.substring(0,maxWidth)+"...";
    }
}
