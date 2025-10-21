package com.backend.domain.product.repository.jpa;

import com.backend.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.bids WHERE p.id = :id")
    Optional<Product> findByIdWithBids(@Param("id") Long id);
    Optional<Product> findFirstByOrderByIdDesc();
}
