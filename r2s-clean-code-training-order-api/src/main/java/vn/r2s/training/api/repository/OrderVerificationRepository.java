package vn.r2s.training.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.r2s.training.api.entity.OrderVerification;

public interface OrderVerificationRepository extends JpaRepository<OrderVerification, Long> {
  Optional<OrderVerification> findByOrderId(Long orderId);
}