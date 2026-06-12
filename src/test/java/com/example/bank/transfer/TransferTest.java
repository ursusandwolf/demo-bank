package com.example.bank.transfer;

import com.example.bank.account.AccountStatus;
import com.example.bank.account.BankAccount;
import com.example.bank.account.BankAccountRepository;
import com.example.bank.account.TransactionRepository;
import com.example.bank.account.dto.TransferRequest;
import com.example.bank.user.Role;
import com.example.bank.user.User;
import com.example.bank.user.UserRepository;
import com.example.bank.user.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        User user1 = createUser("user1@example.com");
        User user2 = createUser("user2@example.com");

        createAccount(user1, "ACC1", new BigDecimal("1000.00"));
        createAccount(user2, "ACC2", new BigDecimal("500.00"));
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void successfulTransfer() throws Exception {
        TransferRequest request = new TransferRequest("ACC1", "ACC2", new BigDecimal("200.00"), "Lunch");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        BankAccount source = accountRepository.findByAccountNumber("ACC1").orElseThrow();
        BankAccount target = accountRepository.findByAccountNumber("ACC2").orElseThrow();

        assertThat(source.getBalance()).isEqualByComparingTo("800.00");
        assertThat(target.getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void transferFromForeignAccountShouldFail() throws Exception {
        // user1 пытается перевести деньги с ACC2, который принадлежит user2
        TransferRequest request = new TransferRequest("ACC2", "ACC1", new BigDecimal("100.00"), "Evil hack");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Мы выбрасываем IllegalArgumentException
    }

    @Test
    @WithUserDetails(value = "user1@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void transferWithInsufficientFundsShouldFail() throws Exception {
        TransferRequest request = new TransferRequest("ACC1", "ACC2", new BigDecimal("2000.00"), "Too expensive");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // IllegalStateException
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
