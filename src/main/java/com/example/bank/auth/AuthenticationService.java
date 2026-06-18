package com.example.bank.auth;

import com.example.bank.auth.dto.AuthenticationResponse;
import com.example.bank.auth.dto.LoginRequest;
import com.example.bank.security.JwtService;
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

    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        var user = userDetailsService.loadUserByUsername(request.email());
        var jwt = jwtService.generateToken(user);
        return new AuthenticationResponse(jwt, "Bearer", 900); // 15 минут в секундах
    }
}
