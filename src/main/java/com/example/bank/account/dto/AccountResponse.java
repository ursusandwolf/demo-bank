package com.example.bank.account.dto;

import com.example.bank.account.AccountStatus;
import com.example.bank.account.BankAccount;
import com.example.bank.user.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class AccountResponse {
    private UUID id;
    private String accountNumber;
    private String ownerFullName;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;

    public static AccountResponse fromEntity(BankAccount account) {
        User owner = account.getOwner();
        String ownerFullName1 = owner.getFirstName() + " " + owner.getLastName();
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .ownerFullName(ownerFullName1)
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }
}
