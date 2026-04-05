package vn.r2s.training.api.service;

import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.dto.response.OrderResponse;

public interface OrderCommandService {
  OrderResponse createOrder(CreateOrderRequest request);
}
