package com.example.bank.auth;

import com.example.bank.auth.dto.AuthenticationResponse;
import com.example.bank.auth.dto.LoginRequest;
import com.example.bank.security.JwtService;
import com.example.bank.security.RefreshToken;
import com.example.bank.security.RefreshTokenRepository;
import com.example.bank.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        var user = userDetailsService.loadUserByUsername(request.email());
        var jwt = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(request.email());
        return new AuthenticationResponse(jwt, refreshToken.getToken(), "Bearer", 900);
    }

    public AuthenticationResponse refresh(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateToken(
                            org.springframework.security.core.userdetails.User.builder()
                                    .username(user.getEmail())
                                    .password(user.getPassword())
                                    .authorities(user.getRoles())
                                    .build()
                    );
                    return new AuthenticationResponse(accessToken, refreshToken, "Bearer", 900);
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
