package vn.r2s.training.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.r2s.training.api.entity.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}