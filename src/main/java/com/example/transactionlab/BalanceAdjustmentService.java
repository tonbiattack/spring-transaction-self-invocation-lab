package com.example.transactionlab;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class BalanceAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(BalanceAdjustmentService.class);

    private final AccountBalanceRepository repository;

    public BalanceAdjustmentService(AccountBalanceRepository repository) {
        this.repository = repository;
    }

    public void adjustAndReject(String accountId, BigDecimal delta) {
        persistAdjustmentThenReject(accountId, delta);
    }

    @Transactional
    public void persistAdjustmentThenReject(String accountId, BigDecimal delta) {
        BigDecimal updatedBalance = repository.findBalance(accountId).add(delta);
        repository.save(accountId, updatedBalance);

        log.info("transactionActive={}, savedBalance={}",
                TransactionSynchronizationManager.isActualTransactionActive(), updatedBalance);

        throw new BalanceAdjustmentRejectedException("業務ルールにより残高調整を取り消します");
    }

    public static class BalanceAdjustmentRejectedException extends RuntimeException {
        public BalanceAdjustmentRejectedException(String message) {
            super(message);
        }
    }
}
