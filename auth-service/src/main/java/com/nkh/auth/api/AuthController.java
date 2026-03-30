package com.nkh.auth.api;

import com.nkh.auth.api.dto.AuthResponse;
import com.nkh.auth.api.dto.ForgotPasswordRequest;
import com.nkh.auth.api.dto.ForgotPasswordResponse;
import com.nkh.auth.api.dto.LoginRequest;
import com.nkh.auth.api.dto.RegisterRequest;
import com.nkh.auth.api.dto.ResetPasswordRequest;
import com.nkh.auth.api.dto.UserSummaryResponse;
import com.nkh.auth.exception.InvalidCredentialsException;
import com.nkh.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    @GetMapping("/users")
    public java.util.List<UserSummaryResponse> users(@RequestParam(name = "query", defaultValue = "") String query) {
        return authService.listUsers(query);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> invalidCredentials(InvalidCredentialsException ex) {
        return Map.of("timestamp", Instant.now(), "status", 401, "error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return Map.of("timestamp", Instant.now(), "status", 400, "error", ex.getMessage());
    }
}
