package com.example.bank.account;

import com.example.bank.user.Role;
import com.example.bank.user.User;
import com.example.bank.user.UserRepository;
import com.example.bank.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем двух пользователей
        User user1 = createUser("user1@example.com");
        User user2 = createUser("user2@example.com");

        // Создаем счет для user1
        BankAccount acc1 = createAccount(user1, "ACC1", new BigDecimal("1000.00"));
        // Создаем счет для user2
        BankAccount acc2 = createAccount(user2, "ACC2", new BigDecimal("500.00"));

        // Добавляем транзакции для ACC1
        createTransaction(acc1, TransactionType.CREDIT, new BigDecimal("1000.00"), new BigDecimal("1000.00"), "Initial deposit");
        createTransaction(acc1, TransactionType.DEBIT, new BigDecimal("100.00"), new BigDecimal("900.00"), "ATM Withdrawal");

        // Добавляем транзакцию для ACC2
        createTransaction(acc2, TransactionType.CREDIT, new BigDecimal("500.00"), new BigDecimal("500.00"), "Salary");
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void userShouldSeeTransactionsOfOwnAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/ACC1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].description", is("Initial deposit")))
                .andExpect(jsonPath("$.content[1].description", is("ATM Withdrawal")));
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void userShouldNotSeeTransactionsOfOtherAccount() throws Exception {
        // Пытаемся получить транзакции ACC2 (принадлежит user2)
        mockMvc.perform(get("/api/accounts/ACC2/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0))); // Возвращаем пустую страницу, так как проверка в запросе не нашла совпадений
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void testPagination() throws Exception {
        // Запрашиваем 1 транзакцию на страницу
        mockMvc.perform(get("/api/accounts/ACC1/transactions")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    private User createUser(String email) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password(passwordEncoder.encode("password"))
                .status(UserStatus.ACTIVE)
                .roles(Set.of(Role.USER))
                .createdAt(Instant.now())
                .build();
        return userRepository.save(user);
    }

    private BankAccount createAccount(User owner, String number, BigDecimal balance) {
        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID())
                .accountNumber(number)
                .owner(owner)
                .balance(balance)
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        return accountRepository.save(account);
    }

    private void createTransaction(BankAccount account, TransactionType type, BigDecimal amount, BigDecimal balanceAfter, String desc) {
        AccountTransaction transaction = AccountTransaction.builder()
                .id(UUID.randomUUID())
                .account(account)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .description(desc)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(transaction);
    }
}
