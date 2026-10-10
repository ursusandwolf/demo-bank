package com.example.bank.security;

import com.example.bank.user.UserRepository;
import com.example.bank.user.User;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_DURATION_DAYS = 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public IssuedToken createRefreshToken(String email) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(userRepository.findByEmail(email).orElseThrow())
                .token(hashToken(rawToken))
                .expiryDate(Instant.now().plus(REFRESH_TOKEN_DURATION_DAYS, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);
        return new IssuedToken(refreshToken.getUser(), rawToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (!token.getExpiryDate().isAfter(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        return token;
    }

    @Transactional
    public void deleteByEmail(String email) {
        refreshTokenRepository.deleteByUserEmail(email);
    }

    @Transactional
    public IssuedToken rotate(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        RefreshToken refreshToken = refreshTokenRepository.findForRotation(hashToken(token))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        verifyExpiration(refreshToken);
        new AccountStatusUserDetailsChecker().check(refreshToken.getUser());
        String replacement = UUID.randomUUID().toString() + UUID.randomUUID();
        refreshToken.setToken(hashToken(replacement));
        refreshToken.setExpiryDate(Instant.now().plus(REFRESH_TOKEN_DURATION_DAYS, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
        return new IssuedToken(refreshToken.getUser(), replacement);
    }

    static String hashToken(String token) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record IssuedToken(User user, String token) {}
}
