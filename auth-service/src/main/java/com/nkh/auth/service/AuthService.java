package com.nkh.auth.service;

import com.nkh.auth.api.dto.AuthResponse;
import com.nkh.auth.api.dto.ForgotPasswordRequest;
import com.nkh.auth.api.dto.ForgotPasswordResponse;
import com.nkh.auth.api.dto.LoginRequest;
import com.nkh.auth.api.dto.RegisterRequest;
import com.nkh.auth.api.dto.ResetPasswordRequest;
import com.nkh.auth.api.dto.UserSummaryResponse;
import com.nkh.auth.domain.PasswordResetTokenEntity;
import com.nkh.auth.domain.UserEntity;
import com.nkh.auth.exception.InvalidCredentialsException;
import com.nkh.auth.repository.PasswordResetTokenRepository;
import com.nkh.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JavaMailSender mailSender;
    private final String passwordResetUrl;
    private final String inboxUrl;
    private final String fromAddress;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            JavaMailSender mailSender,
            @Value("${app.password-reset.frontend-url}") String passwordResetUrl,
            @Value("${app.password-reset.inbox-url}") String inboxUrl,
            @Value("${app.password-reset.from-address}") String fromAddress) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.mailSender = mailSender;
        this.passwordResetUrl = passwordResetUrl;
        this.inboxUrl = inboxUrl;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.username().trim();
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists. Please sign in or choose a different username.");
        }
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists. Please sign in or reset the password.");
        }
        UserEntity user = new UserEntity();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");
        userRepository.save(user);
        return map(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));
        return map(user);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("No profile exists for this email."));

        Instant now = Instant.now();
        passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNullAndExpiresAtAfter(user.getId(), now)
                .forEach(token -> token.setUsedAt(now));

        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setUserId(user.getId());
        resetToken.setToken(UUID.randomUUID() + "-" + UUID.randomUUID());
        resetToken.setExpiresAt(now.plus(15, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(resetToken);
        sendResetEmail(user, resetToken);

        return new ForgotPasswordResponse(
                "Password reset email sent. Open the inbox to copy the token.",
                resetToken.getExpiresAt(),
                inboxUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByToken(request.token().trim())
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid."));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset token has expired. Please request a new one.");
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User account no longer exists."));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public java.util.List<UserSummaryResponse> listUsers(String query) {
        String normalized = query == null ? "" : query.trim();
        return userRepository.findAllByUsernameContainingIgnoreCaseOrderByUsernameAsc(normalized).stream()
                .map(user -> new UserSummaryResponse(user.getId(), user.getUsername(), user.getEmail()))
                .toList();
    }

    private AuthResponse map(UserEntity user) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getRole(), jwtTokenService.issue(user));
    }

    private void sendResetEmail(UserEntity user, PasswordResetTokenEntity token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(user.getEmail());
        message.setSubject("Reset your Docs Collab password");
        message.setText("""
                Hello %s,

                We received a request to reset your password.

                Reset token: %s
                Expires at: %s

                Open the app: %s
                Local inbox: %s
                """.formatted(
                user.getUsername(),
                token.getToken(),
                token.getExpiresAt(),
                passwordResetUrl,
                inboxUrl));
        mailSender.send(message);
    }
}
