package com.example.transactionlab.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProductViewService {
    private static final Logger log = LoggerFactory.getLogger(ProductViewService.class);

    private final ProductMapper productMapper;

    public ProductViewService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public ProductRecord loadThenApplyDisplayOnlyChange(String productId) {
        ProductRecord first = productMapper.findById(productId);
        first.setStatus("DISPLAY_ONLY");
        ProductRecord second = productMapper.findById(productId);
        log.info("mybatisLocalCache transactionActive={}, sameReference={}, firstStatus={}, secondStatus={}",
                TransactionSynchronizationManager.isActualTransactionActive(),
                first == second,
                first.getStatus(),
                second.getStatus());
        return second;
    }
}
