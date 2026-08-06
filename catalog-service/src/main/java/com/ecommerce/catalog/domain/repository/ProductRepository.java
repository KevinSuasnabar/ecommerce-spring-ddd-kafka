package com.ecommerce.catalog.domain.repository;

import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(ProductId productId);

    List<Product> findAll();
}
