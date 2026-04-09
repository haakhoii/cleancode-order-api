package vn.r2s.training.api.event.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.client.NotificationClient;
import vn.r2s.training.api.client.payload.SendNotificationRequest;
import vn.r2s.training.api.enums.NotificationChannel;
import vn.r2s.training.api.event.OrderCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

  private final NotificationClient notificationClient;

  @Async
  @EventListener
  public void handle(OrderCreatedEvent event) {
    log.info("Receive OrderCreatedEvent: orderId={}", event.getOrderId());

    try {
      SendNotificationRequest request = SendNotificationRequest.builder()
          .content("Order successfully")
          .to(event.getCustomerEmail())
          .channel(NotificationChannel.EMAIL.name())
          .build();

      notificationClient.sendNotification(request);

    } catch (Exception e) {
      log.error("Send notification failed for orderId={}", event.getOrderId());
    }
  }
}