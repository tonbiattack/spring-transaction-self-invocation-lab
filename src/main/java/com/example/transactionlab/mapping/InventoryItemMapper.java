package com.example.transactionlab.mapping;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryItemMapper {
    InventoryItem findById(String itemId);
}
