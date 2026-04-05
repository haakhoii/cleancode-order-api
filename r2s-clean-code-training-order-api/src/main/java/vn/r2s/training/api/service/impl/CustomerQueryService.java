package vn.r2s.training.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.entity.CustomerEntity;
import vn.r2s.training.api.exception.NotFoundException;
import vn.r2s.training.api.repository.CustomerRepository;

@Service
@RequiredArgsConstructor
public class CustomerQueryService {

  private final CustomerRepository customerRepo;

  public CustomerEntity getByEmail(String email) {
    return customerRepo.findByEmail(email)
        .orElseThrow(() -> new NotFoundException("Customer not found"));
  }
}