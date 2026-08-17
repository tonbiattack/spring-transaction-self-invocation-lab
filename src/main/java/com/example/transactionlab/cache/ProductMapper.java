package com.example.transactionlab.cache;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper {
    ProductRecord findById(String productId);
}
