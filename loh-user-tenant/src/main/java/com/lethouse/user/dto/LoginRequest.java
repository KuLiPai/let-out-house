package com.lethouse.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 64)
    private String tenantCode;
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64,message = "用户名不能过长")
    private String username;
    @NotBlank(message = "密码不能为空")
    @Size(min=6,max=32,message = "密码长度必须是6-32位")
    private String password;
}

