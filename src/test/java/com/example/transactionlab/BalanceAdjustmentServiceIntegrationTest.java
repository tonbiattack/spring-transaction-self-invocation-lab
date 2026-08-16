package com.example.transactionlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BalanceAdjustmentServiceIntegrationTest {

    @Autowired
    private BalanceAdjustmentService service;

    @Autowired
    private AccountBalanceRepository repository;

    @BeforeEach
    void setUp() {
        repository.save("account-001", new BigDecimal("100.00"));
    }

    @Test
    void 拒否された残高調整は例外後も永続化されてはならない() {
        assertThatThrownBy(() -> service.adjustAndReject("account-001", new BigDecimal("25.00")))
                .isInstanceOf(BalanceAdjustmentService.BalanceAdjustmentRejectedException.class)
                .hasMessage("業務ルールにより残高調整を取り消します");

        assertThat(repository.findBalance("account-001"))
                .as("例外後にDBを再読込しても、ロールバック後の残高であること")
                .isEqualByComparingTo("100.00");
    }
}
