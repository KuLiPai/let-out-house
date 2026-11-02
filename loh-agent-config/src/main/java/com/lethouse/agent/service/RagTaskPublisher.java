package com.lethouse.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagTaskPublisher {

    private final StringRedisTemplate redisTemplate;

    @Value("${loh.rag.stream-key}")
    private String streamKey;

    public void publishDocParse(Long docId,Long kbId,Long tenantId,String filePath,String fileType,String docName){
        Map<String ,String > msg=new HashMap<>();
        msg.put("docId",String.valueOf(docId));
        msg.put("kbId",String.valueOf(kbId));
        msg.put("tenantId",String.valueOf(tenantId));
        msg.put("filePath",filePath);
        msg.put("fileType",fileType);
        msg.put("docName",docName==null?"":docName);
        msg.put("traceId", MDC.get("traceId")==null?"":MDC.get("traceId"));

        MapRecord<String, String, String> record = StreamRecords.mapBacked(msg).withStreamKey(streamKey);
        var msgId = redisTemplate.opsForStream().add(record);
        log.info("已投递解析任务：docId={},msgId={}",docId,msgId);
    }

}
