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
class AccountAuthorizationTest {

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
        createAccount(user1, "ACC1", new BigDecimal("1000.00"));
        // Создаем счет для user2
        createAccount(user2, "ACC2", new BigDecimal("500.00"));
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void userShouldSeeOnlyOwnAccounts() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountNumber", is("ACC1")));
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void userShouldAccessOwnAccountDetails() throws Exception {
        mockMvc.perform(get("/api/accounts/ACC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber", is("ACC1")))
                .andExpect(jsonPath("$.balance", is(1000.00)));
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void userShouldNotAccessOtherUserAccount() throws Exception {
        // Пытаемся получить доступ к ACC2 (владелец - user2)
        mockMvc.perform(get("/api/accounts/ACC2"))
                .andExpect(status().isNotFound()); // Возвращаем 404, скрывая существование чужого счета
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

    private void createAccount(User owner, String number, BigDecimal balance) {
        BankAccount account = BankAccount.builder()
                .id(UUID.randomUUID())
                .accountNumber(number)
                .owner(owner)
                .balance(balance)
                .currency("USD")
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
        accountRepository.save(account);
    }
}
