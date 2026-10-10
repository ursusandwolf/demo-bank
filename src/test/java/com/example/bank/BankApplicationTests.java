package com.example.bank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
@ActiveProfiles("test")
class BankApplicationTests {

    @Test
    void contextLoads() {
    }

}
