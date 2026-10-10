package com.example.bank.auth;

import com.example.bank.auth.dto.AuthenticationResponse;
import com.example.bank.auth.dto.LoginRequest;
import com.example.bank.security.JwtService;
import com.example.bank.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        var user = (UserDetails) authentication.getPrincipal();
        var jwt = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(request.email());
        return new AuthenticationResponse(jwt, refreshToken.token(), "Bearer", 900);
    }

    public AuthenticationResponse refresh(String refreshToken) {
        var rotated = refreshTokenService.rotate(refreshToken);
        return new AuthenticationResponse(jwtService.generateToken(rotated.user()),
                rotated.token(), "Bearer", 900);
    }
}
