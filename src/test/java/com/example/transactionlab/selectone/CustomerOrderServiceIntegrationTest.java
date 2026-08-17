package com.example.transactionlab.selectone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class CustomerOrderServiceIntegrationTest {
    @Autowired private CustomerOrderService service;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM customer_orders");
        jdbcTemplate.update("INSERT INTO customer_orders VALUES (?, ?, ?, ?)", "order-001", "customer-001", "2026-08-16T10:00:00", 1000);
        jdbcTemplate.update("INSERT INTO customer_orders VALUES (?, ?, ?, ?)", "order-002", "customer-001", "2026-08-17T10:00:00", 2000);
    }

    @Test
    void 最新注文を1件返す() {
        assertThat(service.findLatest("customer-001").getOrderId()).isEqualTo("order-002");
    }
}
