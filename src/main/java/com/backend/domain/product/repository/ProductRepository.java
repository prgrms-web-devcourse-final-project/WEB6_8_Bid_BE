package com.backend.domain.product.repository;

import com.backend.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    Optional<Product> findFirstByOrderByIdDesc();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.productImages WHERE p.id = :productId")
    Optional<Product> findByIdWithImages(Long productId);
}
