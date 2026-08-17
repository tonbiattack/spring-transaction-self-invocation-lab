package com.example.transactionlab.mapping;

import org.springframework.stereotype.Service;

@Service
public class InventoryItemService {
    private final InventoryItemMapper mapper;

    public InventoryItemService(InventoryItemMapper mapper) {
        this.mapper = mapper;
    }

    public InventoryItem find(String itemId) {
        return mapper.findById(itemId);
    }
}
