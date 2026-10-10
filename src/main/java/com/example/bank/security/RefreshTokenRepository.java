package com.example.bank.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from RefreshToken t where t.token = :token")
    Optional<RefreshToken> findForRotation(@org.springframework.data.repository.query.Param("token") String token);
    void deleteByUserEmail(String email);
}
