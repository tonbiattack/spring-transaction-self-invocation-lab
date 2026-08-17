package com.example.transactionlab.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ProductViewServiceIntegrationTest {
    @Autowired
    private ProductViewService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("INSERT INTO products (product_id, status) VALUES (?, ?)", "product-001", "ACTIVE");
    }

    @Test
    void 表示用の変更でDB再読込結果まで変わってはならない() {
        ProductRecord result = service.loadThenApplyDisplayOnlyChange("product-001");

        assertThat(result.getStatus())
                .as("DBから2回目に取得した商品状態は元のACTIVEであること")
                .isEqualTo("ACTIVE");
    }
}
