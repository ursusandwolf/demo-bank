package com.example.bank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {
    private final SecurityAuditRepository events;

    @Transactional
    public void record(SecurityAuditEvent.Type type, UUID actor, UUID target) {
        events.save(new SecurityAuditEvent(type, actor, target));
    }

    /** Failed attempts must survive rollback of the rejected operation. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(SecurityAuditEvent.Type type, UUID actor) {
        events.save(new SecurityAuditEvent(type, actor, null));
    }
}
