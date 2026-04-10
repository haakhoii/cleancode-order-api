package vn.r2s.training.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.r2s.training.api.dto.response.OrderResponse;

@Getter
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
  private final OrderResponse order;
}
