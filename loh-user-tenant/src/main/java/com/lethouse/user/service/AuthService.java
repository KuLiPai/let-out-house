package com.lethouse.user.service;

import com.lethouse.user.dto.LoginRequest;
import com.lethouse.user.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest req);
}
