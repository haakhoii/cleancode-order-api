package vn.r2s.training.api.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.dto.response.OrderItemsResponse;
import vn.r2s.training.api.dto.response.OrderResponse;
import vn.r2s.training.api.entity.OrderEntity;
import vn.r2s.training.api.exception.BadRequestException;
import vn.r2s.training.api.exception.NotFoundException;
import vn.r2s.training.api.mapper.OrderMapper;
import vn.r2s.training.api.repository.OrderRepository;
import vn.r2s.training.api.service.OrderQueryService;

@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {
  private final OrderRepository orderRepo;
  private final OrderMapper orderMapper;

  @Override
  public OrderResponse getOrder(Long id) {
    if (id == null || id <= 0) {
      throw new BadRequestException("invalid id: " + id);
    }

    OrderEntity order = orderRepo.findById(id)
        .orElseThrow(() -> new NotFoundException("Order not found: " + id));

    return orderMapper.toResponse(order);
  }
}
