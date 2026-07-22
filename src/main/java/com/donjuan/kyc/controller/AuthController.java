package com.donjuan.kyc.controller;

import com.donjuan.kyc.dto.LoginRequest;
import com.donjuan.kyc.dto.LoginResponse;
import com.donjuan.kyc.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final String authUsername;
    private final String authPasswordHash;

    public AuthController(JwtService jwtService,
                           PasswordEncoder passwordEncoder,
                           @Value("${kyc.auth.username}") String authUsername,
                           @Value("${kyc.auth.password-hash}") String authPasswordHash) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authUsername = authUsername;
        this.authPasswordHash = authPasswordHash;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        boolean userOk = authUsername.equals(req.getUsername());
        boolean passOk = passwordEncoder.matches(req.getPassword(), authPasswordHash);

        if (!userOk || !passOk) {
            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("error", "usuario ou senha invalidos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        String token = jwtService.generateToken(authUsername);
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
