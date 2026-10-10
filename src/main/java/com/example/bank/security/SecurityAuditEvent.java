package com.example.bank.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

/** Security metadata only: never store request bodies, passwords or tokens. */
@Entity
@Table(name = "security_audit_events")
@Getter
@NoArgsConstructor
public class SecurityAuditEvent {
    public enum Type { LOGIN_SUCCESS, LOGIN_FAILURE, LOGIN_THROTTLED, REFRESH_SUCCESS, REFRESH_FAILURE,
        LOGOUT, PASSWORD_CHANGED, PASSWORD_CHANGE_FAILURE, USER_BLOCKED, USER_ACTIVATED }

    @Id private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private Type type;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "target_id") private UUID targetId;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    public SecurityAuditEvent(Type type, UUID actorId, UUID targetId) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.actorId = actorId;
        this.targetId = targetId;
        this.occurredAt = Instant.now();
    }
}
