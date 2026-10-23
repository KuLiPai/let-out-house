package com.lethouse.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant implements Serializable {

    private Long id;
    private String tenantCode;
    private String tenantName;
    private String contactName;
    private String contactPhone;
    private Integer status;
    private LocalDateTime expireAt;
    private String remark;
    private Long deleted;
}
