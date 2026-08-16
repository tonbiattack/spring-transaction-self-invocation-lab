package com.example.transactionlab;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class BalanceAdjustmentService {

    private final TransactionalBalanceWriter transactionalBalanceWriter;

    public BalanceAdjustmentService(TransactionalBalanceWriter transactionalBalanceWriter) {
        this.transactionalBalanceWriter = transactionalBalanceWriter;
    }

    public void adjustAndReject(String accountId, BigDecimal delta) {
        transactionalBalanceWriter.persistAdjustmentThenReject(accountId, delta);
    }

    public static class BalanceAdjustmentRejectedException extends RuntimeException {
        public BalanceAdjustmentRejectedException(String message) {
            super(message);
        }
    }
}
