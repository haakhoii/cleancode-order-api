package vn.r2s.training.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vn.r2s.training.api.enums.CustomerType;

@Getter
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
  private final Long orderId;
  private final String customerEmail;
  private final CustomerType customerType;
}
