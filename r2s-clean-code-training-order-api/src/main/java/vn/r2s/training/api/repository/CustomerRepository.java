package vn.r2s.training.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import vn.r2s.training.api.entity.CustomerEntity;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
  Optional<CustomerEntity> findByEmail(String email);
}