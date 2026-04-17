package vn.r2s.training.api.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.kafka.event.OrderCreatedEvent;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

  private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

  private static final String TOPIC = "order-created";

  public void send(OrderCreatedEvent event) {
    kafkaTemplate.send(TOPIC, event);
  }
}