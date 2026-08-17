package com.example.transactionlab.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class InventoryItemServiceIntegrationTest {
    @Autowired private InventoryItemService service;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM inventory_items");
        jdbcTemplate.update("INSERT INTO inventory_items VALUES (?, ?)", "item-001", "Mechanical Keyboard");
    }

    @Test
    void snake_case列をJavaのcamelCaseプロパティへマッピングする() {
        InventoryItem item = service.find("item-001");
        assertThat(item.getItemId()).isEqualTo("item-001");
        assertThat(item.getDisplayName()).isEqualTo("Mechanical Keyboard");
    }
}
