package com.example.bank.account;

import com.example.bank.account.dto.AccountResponse;
import com.example.bank.account.dto.TransactionResponse;
import com.example.bank.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public List<AccountResponse> getMyAccounts(@AuthenticationPrincipal User user) {
        return accountRepository.findAllByOwnerEmail(user.getEmail())
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountDetails(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal User user) {
        
        return accountRepository.findByAccountNumberAndOwnerEmail(accountNumber, user.getEmail())
                .map(AccountResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // 404 если не найден или не принадлежит пользователю
    }

    @GetMapping("/{accountNumber}/transactions")
    public Page<TransactionResponse> getAccountTransactions(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        
        return transactionRepository.findAllByAccountAccountNumberAndAccountOwnerEmail(
                accountNumber, user.getEmail(), pageable)
                .map(TransactionResponse::fromEntity);
    }
}
