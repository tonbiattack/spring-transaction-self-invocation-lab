package com.example.transactionlab.dynamicsql;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductSearchService {
    private final ProductSearchMapper productSearchMapper;

    public ProductSearchService(ProductSearchMapper productSearchMapper) {
        this.productSearchMapper = productSearchMapper;
    }

    public List<CatalogProduct> findSelectedProducts(List<String> productIds) {
        return productSearchMapper.findByIds(productIds);
    }
}
