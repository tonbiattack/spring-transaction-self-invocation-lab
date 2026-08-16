package com.example.transactionlab;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountBalanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountBalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS account_balance (
                    account_id VARCHAR(40) PRIMARY KEY,
                    balance DECIMAL(19, 2) NOT NULL
                )
                """);
    }

    public void save(String accountId, BigDecimal balance) {
        jdbcTemplate.update(
                "MERGE INTO account_balance (account_id, balance) KEY(account_id) VALUES (?, ?)",
                accountId,
                balance
        );
    }

    public BigDecimal findBalance(String accountId) {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM account_balance WHERE account_id = ?",
                BigDecimal.class,
                accountId
        );
    }
}
