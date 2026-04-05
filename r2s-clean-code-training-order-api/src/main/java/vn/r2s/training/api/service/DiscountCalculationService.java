package vn.r2s.training.api.service;

import vn.r2s.training.api.dto.request.CreateOrderRequest;

public interface DiscountCalculationService {
  int calculate(int totalCents, CreateOrderRequest request);
}
