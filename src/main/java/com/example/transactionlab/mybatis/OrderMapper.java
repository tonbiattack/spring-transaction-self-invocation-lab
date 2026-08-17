package com.example.transactionlab.mybatis;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    void insert(OrderRecord order);
    int countById(String orderId);
}
