package com.app.auth.controller;

import com.app.auth.dto.*;
import com.app.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody AuthRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.signup(
                        req.getFullname(),
                        req.getEmail(),
                        req.getPassword(),
                        req.getRole(),
                        req.getSpecialization(),
                        req.getClinicName()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto req) {
        return ResponseEntity.ok(service.login(req.getEmail(), req.getPassword()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequestDto req,
                                                 org.springframework.security.core.Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(service.changePassword(userId, req.getCurrentPassword(), req.getNewPassword()));
    }

}
