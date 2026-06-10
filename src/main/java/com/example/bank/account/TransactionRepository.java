package com.example.bank.account;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<AccountTransaction, UUID> {

    // Безопасное получение транзакций: проверяем и номер счета, и email владельца
    Page<AccountTransaction> findAllByAccountAccountNumberAndAccountOwnerEmail(
            String accountNumber, 
            String ownerEmail, 
            Pageable pageable
    );
}
