package com.example.transactionlab.selectone;

import org.springframework.stereotype.Service;

@Service
public class CustomerOrderService {
    private final CustomerOrderMapper mapper;

    public CustomerOrderService(CustomerOrderMapper mapper) {
        this.mapper = mapper;
    }

    public CustomerOrder findLatest(String customerId) {
        return mapper.findLatestByCustomerId(customerId);
    }
}
