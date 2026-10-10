package com.example.bank.auth;

import com.example.bank.auth.dto.AuthenticationResponse;
import com.example.bank.auth.dto.LoginRequest;
import com.example.bank.security.JwtService;
import com.example.bank.security.RefreshTokenService;
import com.example.bank.security.SecurityAuditService;
import com.example.bank.security.SecurityAuditEvent.Type;
import com.example.bank.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository users;
    private final SecurityAuditService audit;

    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        try {
            // Serialize issuance with logout/password changes, including authentication itself.
            users.findForSecurityUpdate(request.email());
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            var user = (UserDetails) authentication.getPrincipal();
            var jwt = jwtService.generateToken(user);
            var refreshToken = refreshTokenService.createRefreshToken(request.email());
            audit.record(Type.LOGIN_SUCCESS, refreshToken.user().getId(), refreshToken.user().getId());
            return new AuthenticationResponse(jwt, refreshToken.token(), "Bearer", 900);
        } catch (AuthenticationException ex) {
            audit.recordFailure(Type.LOGIN_FAILURE, null);
            throw ex;
        }
    }

    @Transactional
    public AuthenticationResponse refresh(String refreshToken) {
        try {
            var rotated = refreshTokenService.rotate(refreshToken);
            audit.record(Type.REFRESH_SUCCESS, rotated.user().getId(), rotated.user().getId());
            return new AuthenticationResponse(jwtService.generateToken(rotated.user()),
                    rotated.token(), "Bearer", 900);
        } catch (AuthenticationException ex) {
            audit.recordFailure(Type.REFRESH_FAILURE, null);
            throw ex;
        }
    }
}
