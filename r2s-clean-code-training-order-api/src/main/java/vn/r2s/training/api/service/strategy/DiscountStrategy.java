package vn.r2s.training.api.service.strategy;

import vn.r2s.training.api.dto.request.CreateOrderRequest;

public interface DiscountStrategy {
  boolean isApplicable(CreateOrderRequest request);
  int calculate(int totalCents, CreateOrderRequest request);
}
