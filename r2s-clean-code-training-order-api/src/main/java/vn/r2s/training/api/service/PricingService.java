package vn.r2s.training.api.service;

import java.util.List;
import vn.r2s.training.api.dto.request.OrderItemRequest;
import vn.r2s.training.api.dto.response.PricingResponse;

public interface PricingService {
  PricingResponse calculate(List<OrderItemRequest> items);
}
