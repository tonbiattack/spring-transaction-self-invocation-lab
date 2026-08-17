package com.example.transactionlab.dynamicsql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ProductSearchServiceIntegrationTest {
    @Autowired
    private ProductSearchService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM products");
        jdbcTemplate.update("INSERT INTO products (product_id, status) VALUES (?, ?)", "product-001", "ACTIVE");
        jdbcTemplate.update("INSERT INTO products (product_id, status) VALUES (?, ?)", "product-002", "INACTIVE");
    }

    @Test
    void 空の選択は構文エラーではなく0件になる() {
        assertThat(service.findSelectedProducts(List.of()))
                .as("選択IDが空なら、全件ではなく0件を返すこと")
                .isEmpty();
    }

    @Test
    void 選択されたIDだけを返す() {
        assertThat(service.findSelectedProducts(List.of("product-002")))
                .extracting(CatalogProduct::getProductId)
                .containsExactly("product-002");
    }
}
