package vn.r2s.training.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import vn.r2s.training.api.entity.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
  Optional<ProductEntity> findBySku(String sku);
}
