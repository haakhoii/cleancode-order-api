package vn.r2s.training.api.service.strategy;

import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.enums.DiscountCategory;

public interface DiscountStrategy {
  DiscountCategory category();
  boolean isApplicable(CreateOrderRequest request);
  int calculate(int totalCents, CreateOrderRequest request);
}
