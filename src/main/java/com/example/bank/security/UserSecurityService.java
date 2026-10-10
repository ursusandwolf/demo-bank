package com.example.bank.security;

import com.example.bank.auth.dto.ChangePasswordRequest;
import com.example.bank.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSecurityService {
    private final UserRepository users;
    private final RefreshTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final SecurityAuditService audit;

    @Transactional
    public void logout(User principal) {
        User user = currentUser(principal);
        revoke(user);
        audit.record(SecurityAuditEvent.Type.LOGOUT, user.getId(), user.getId());
    }

    @Transactional
    public void changePassword(User principal, ChangePasswordRequest request) {
        User user = currentUser(principal);
        if (!encoder.matches(request.currentPassword(), user.getPassword())) {
            audit.recordFailure(SecurityAuditEvent.Type.PASSWORD_CHANGE_FAILURE, user.getId());
            throw new BadCredentialsException("Invalid current password");
        }
        if (encoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must differ from the current password");
        }
        user.setPassword(encoder.encode(request.newPassword()));
        revoke(user);
        audit.record(SecurityAuditEvent.Type.PASSWORD_CHANGED, user.getId(), user.getId());
    }

    @Transactional
    public void setStatus(User principal, UUID targetId, UserStatus status) {
        // Check the caller again inside the transaction, including token revocation.
        User actor = users.findByEmail(principal.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
        checkPrincipal(principal, actor);
        if (!actor.getRoles().contains(Role.ADMIN)) {
            throw new org.springframework.security.access.AccessDeniedException("Administrator required");
        }
        User target = users.findForSecurityUpdateById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // Lock only the target: every lifecycle operation locks user before refresh rows.
        target.setStatus(status);
        revoke(target);
        audit.record(status == UserStatus.BLOCKED ? SecurityAuditEvent.Type.USER_BLOCKED
                : SecurityAuditEvent.Type.USER_ACTIVATED, actor.getId(), target.getId());
    }

    private User currentUser(User principal) {
        User user = users.findForSecurityUpdate(principal.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Unknown user"));
        checkPrincipal(principal, user);
        return user;
    }

    private void checkPrincipal(User principal, User user) {
        new AccountStatusUserDetailsChecker().check(user);
        if (principal.getAuthVersion() != user.getAuthVersion()) {
            throw new BadCredentialsException("Revoked authentication");
        }
    }

    private void revoke(User user) {
        user.setAuthVersion(user.getAuthVersion() + 1);
        tokens.deleteByUserEmail(user.getEmail());
    }
}
