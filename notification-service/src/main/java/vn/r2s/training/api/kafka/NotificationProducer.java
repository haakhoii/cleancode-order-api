package vn.r2s.training.api.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.request.SendNotificationRequestDto;

@Component
@RequiredArgsConstructor
public class NotificationProducer {

  private final KafkaTemplate<String, SendNotificationRequestDto> kafkaTemplate;

  private static final String TOPIC = "send-notification";

  public void send(SendNotificationRequestDto request) {
    kafkaTemplate.send(TOPIC, request);
  }
}