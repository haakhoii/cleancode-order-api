package vn.r2s.training.api.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class OrderCreatedEvent {
  private String to;
  private String subject;
  private String content;
}