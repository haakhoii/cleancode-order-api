package vn.r2s.training.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {

  private final Long orderId;
  private final String customerEmail;
  private final int total;
  private final int discount;
}
