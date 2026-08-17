package com.example.transactionlab.dynamicsql;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductSearchMapper {
    List<CatalogProduct> findByIds(@Param("productIds") List<String> productIds);
}
