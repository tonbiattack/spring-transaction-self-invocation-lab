package com.example.transactionlab.mybatis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderRegistrationService {
    private static final Logger log = LoggerFactory.getLogger(OrderRegistrationService.class);

    private final OrderMapper orderMapper;

    public OrderRegistrationService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Transactional
    public void registerThenReject(String orderId) throws OrderRejectedException {
        orderMapper.insert(new OrderRecord(orderId, "PENDING"));
        log.info("mybatisInsert transactionActive={}, orderId={}",
                TransactionSynchronizationManager.isActualTransactionActive(), orderId);
        throw new OrderRejectedException("在庫確認に失敗したため注文を取り消します");
    }
}
