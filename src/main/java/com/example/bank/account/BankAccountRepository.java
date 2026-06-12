package com.example.bank.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    // Безопасный поиск: всегда добавляем ownerEmail в условие
    @Query("SELECT a FROM BankAccount a WHERE a.accountNumber = :accountNumber AND a.owner.email = :ownerEmail")
    Optional<BankAccount> findByAccountNumberAndOwnerEmail(String accountNumber, String ownerEmail);

    // Список всех счетов конкретного пользователя
    @Query("SELECT a FROM BankAccount a WHERE a.owner.email = :ownerEmail")
    List<BankAccount> findAllByOwnerEmail(String ownerEmail);

    Optional<BankAccount> findByAccountNumber(String accountNumber);
}
