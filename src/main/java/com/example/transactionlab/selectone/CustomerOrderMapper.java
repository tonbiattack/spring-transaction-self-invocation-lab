package com.example.transactionlab.selectone;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerOrderMapper {
    CustomerOrder findLatestByCustomerId(String customerId);
}
