package com.example.bank.auth;

import com.example.bank.auth.dto.AuthenticationResponse;
import com.example.bank.auth.dto.LoginRequest;
import com.example.bank.auth.dto.ChangePasswordRequest;
import com.example.bank.security.UserSecurityService;
import com.example.bank.user.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserSecurityService userSecurityService;

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        userSecurityService.logout(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        userSecurityService.changePassword(user, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(@RequestBody String refreshToken) {
        return ResponseEntity.ok(authenticationService.refresh(refreshToken));
    }
}
