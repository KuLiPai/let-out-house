package com.lethouse.user.controller;

import com.lethouse.common.result.R;
import com.lethouse.user.dto.LoginRequest;
import com.lethouse.user.dto.LoginResponse;
import com.lethouse.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody @Valid LoginRequest request){
        return R.ok(authService.login(request));
    }
}
