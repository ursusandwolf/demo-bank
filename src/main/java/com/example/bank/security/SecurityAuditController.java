package com.example.bank.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/security-events")
@RequiredArgsConstructor
public class SecurityAuditController {
    private final SecurityAuditRepository events;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<EventResponse> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size between 1 and 100");
        }
        return events.findAll(PageRequest.of(page, size, Sort.by("occurredAt").descending()
                .and(Sort.by("id")))).map(event -> new EventResponse(event.getId(), event.getType(),
                event.getActorId(), event.getTargetId(), event.getOccurredAt()));
    }

    public record EventResponse(UUID id, SecurityAuditEvent.Type type, UUID actorId,
                                UUID targetId, Instant occurredAt) {}
}
