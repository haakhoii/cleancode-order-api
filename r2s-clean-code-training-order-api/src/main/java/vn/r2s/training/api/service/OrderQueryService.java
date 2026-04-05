package vn.r2s.training.api.service;

import vn.r2s.training.api.dto.response.OrderResponse;

public interface OrderQueryService {
  OrderResponse getOrder(Long id);
}
