package com.lethouse.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("agent_config")
public class AgentRef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer status;
    private Long deleted;
}
