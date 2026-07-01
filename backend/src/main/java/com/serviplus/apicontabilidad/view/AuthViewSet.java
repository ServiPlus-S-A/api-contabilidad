package com.serviplus.apicontabilidad.view;

import com.serviplus.apicontabilidad.logic.AuthService;
import com.serviplus.apicontabilidad.serializer.auth.LoginRequest;
import com.serviplus.apicontabilidad.serializer.auth.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthViewSet {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.username(), request.password()));
    }
}
