package com.lethouse.user.dto;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long userId;
    private Long tenantId;
    private String username;
    private String nickname;
}
