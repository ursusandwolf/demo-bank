package com.example.bank.transfer;

import com.example.bank.account.*;
import com.example.bank.account.dto.TransferRequest;
import com.example.bank.user.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class TransferPersistenceTest {
    @Autowired TransferService transfers;
    @Autowired BankAccountRepository accounts;
    @Autowired UserRepository users;
    @Autowired TransactionTemplate transactions;
    @Autowired EntityManagerFactory factory;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    @MockitoSpyBean TransactionRepository history;
    private User owner;
    private BankAccount source;
    private BankAccount target;

    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            owner = users.save(User.builder().email(UUID.randomUUID() + "@example.com")
                    .password("unused-in-this-test").roles(Set.of(Role.USER)).build());
            source = accounts.save(account("S", "100.00"));
            target = accounts.save(account("T", "50.00"));
        });
    }

    private BankAccount account(String prefix, String balance) {
        return BankAccount.builder().id(UUID.randomUUID())
                .accountNumber(prefix + UUID.randomUUID().toString().substring(0, 12))
                .owner(owner).balance(new BigDecimal(balance)).currency("USD").status(AccountStatus.ACTIVE).build();
    }

    @AfterEach
    void tearDown() {
        reset(history);
        transactions.executeWithoutResult(status -> {
            history.deleteAll(history.findAll().stream().filter(tx ->
                    tx.getAccount().getId().equals(source.getId()) || tx.getAccount().getId().equals(target.getId())).toList());
            history.flush();
            accounts.deleteById(source.getId());
            accounts.deleteById(target.getId());
            accounts.flush();
            users.deleteById(owner.getId());
        });
    }

    @Test
    void failureWritingSecondHistoryRecordRollsBackBothBalancesAndFirstRecord() {
        var calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) throw new IllegalStateException("Simulated history failure");
            AccountTransaction record = invocation.getArgument(0);
            entityManager.persist(record);
            entityManager.flush();
            return record;
        }).when(history).save(any(AccountTransaction.class));

        assertThatThrownBy(() -> transfers.transfer(new TransferRequest(source.getAccountNumber(),
                target.getAccountNumber(), new BigDecimal("10.00"), "Rollback"), owner))
                .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Simulated history failure");
        assertThat(accounts.findById(source.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(accounts.findById(target.getId()).orElseThrow().getBalance()).isEqualByComparingTo("50.00");
        assertThat(history.findAll()).noneMatch(tx -> tx.getAccount().getId().equals(source.getId())
                || tx.getAccount().getId().equals(target.getId()));
    }

    @Test
    void staleAccountUpdateCannotOverwriteCommittedBalance() {
        try (var first = factory.createEntityManager(); var second = factory.createEntityManager()) {
            first.getTransaction().begin();
            second.getTransaction().begin();
            var firstRead = first.find(BankAccount.class, source.getId());
            var staleRead = second.find(BankAccount.class, source.getId());
            firstRead.setBalance(new BigDecimal("90.00"));
            first.getTransaction().commit();
            staleRead.setBalance(new BigDecimal("80.00"));
            assertThatThrownBy(second::flush).isInstanceOf(OptimisticLockException.class);
            second.getTransaction().rollback();
        }
        assertThat(accounts.findById(source.getId()).orElseThrow().getBalance()).isEqualByComparingTo("90.00");
    }
}
