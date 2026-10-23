package com.lethouse.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("sys_role")

public class SysRole implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer builtin;
    private Integer status;
    private Long deleted;

}
