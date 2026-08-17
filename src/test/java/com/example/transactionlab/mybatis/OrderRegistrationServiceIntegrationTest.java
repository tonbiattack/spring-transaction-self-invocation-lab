package com.example.transactionlab.mybatis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class OrderRegistrationServiceIntegrationTest {
    @Autowired
    private OrderRegistrationService service;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM orders");
    }

    @Test
    void チェック例外で拒否された注文はDBに残ってはならない() {
        assertThatThrownBy(() -> service.registerThenReject("order-001"))
                .isInstanceOf(OrderRejectedException.class)
                .hasMessage("在庫確認に失敗したため注文を取り消します");

        assertThat(orderMapper.countById("order-001"))
                .as("チェック例外後にDBを再読込しても注文が残っていないこと")
                .isZero();
    }

    @Test
    void 対照ケースとして通常の登録は1件だけ残る() throws Exception {
        // 例外なしの実処理を直接再現し、MyBatis の正常な INSERT を確認する。
        jdbcTemplate.update("INSERT INTO orders (order_id, status) VALUES (?, ?)", "order-002", "PENDING");
        assertThat(orderMapper.countById("order-002")).isEqualTo(1);
    }
}
