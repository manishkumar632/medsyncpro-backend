package com.medsyncpro.controller;

import com.medsyncpro.dto.LoginRequest;
import com.medsyncpro.dto.LoginResponse;
import com.medsyncpro.dto.RegisterRequest;
import com.medsyncpro.entity.User;
import com.medsyncpro.response.ApiResponse;
import com.medsyncpro.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
}
