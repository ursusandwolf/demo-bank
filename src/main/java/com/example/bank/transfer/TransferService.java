package com.example.bank.transfer;

import com.example.bank.account.*;
import com.example.bank.account.dto.TransferRequest;
import com.example.bank.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void transfer(TransferRequest request, User currentUser) {
        // 1. Безопасность: Проверяем, что исходный счет принадлежит текущему пользователю
        // Мы используем findByAccountNumberAndOwnerEmail, чтобы злоумышленник не мог списать деньги с чужого счета
        BankAccount sourceAccount = accountRepository.findByAccountNumberAndOwnerEmail(
                request.fromAccountNumber(), currentUser.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found or access denied"));

        // 2. Поиск целевого счета (он может принадлежать кому угодно)
        BankAccount targetAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));
        
        // ВАЖНО: Мы не проверяем владельца для целевого счета, так как переводить можно кому угодно.

        // 3. Бизнес-валидация
        validateTransfer(sourceAccount, targetAccount, request.amount());

        // 4. Обновление балансов
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.amount()));

        // 5. Сохранение (Hibernate проверит @Version здесь при коммите транзакции)
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 6. Логирование операций
        createTransaction(sourceAccount, TransactionType.TRANSFER_OUT, request.amount(), request.description());
        createTransaction(targetAccount, TransactionType.TRANSFER_IN, request.amount(), request.description());
    }

    private void validateTransfer(BankAccount source, BankAccount target, BigDecimal amount) {
        if (source.getAccountNumber().equals(target.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (source.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active");
        }
        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Target account is not active");
        }
        if (!source.getCurrency().equals(target.getCurrency())) {
            throw new IllegalStateException("Currency mismatch");
        }
        if (source.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
    }

    private void createTransaction(BankAccount account, TransactionType type, BigDecimal amount, String description) {
        AccountTransaction tx = AccountTransaction.builder()
                .id(UUID.randomUUID())
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .description(description)
                .build();
        transactionRepository.save(tx);
    }
}
