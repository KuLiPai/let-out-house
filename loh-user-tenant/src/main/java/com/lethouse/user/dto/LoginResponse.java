package com.lethouse.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private Long tenantId;
    private String tenantCode;
    private String username;
    private String nickname;
    private List<String> roleList;

}
