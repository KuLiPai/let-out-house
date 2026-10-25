package com.lethouse.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String kbCode;
    private String kbName;
    private String description;
    private String embeddingModel;
    private String collectionName;
    private Integer docCount;
    private Integer status;
    private Long deleted;
}
