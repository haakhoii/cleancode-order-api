package vn.r2s.training.api.event.listener;

import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.client.NotificationClient;
import vn.r2s.training.api.client.payload.SendNotificationRequest;
import vn.r2s.training.api.dto.response.OrderResponse;
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
    OrderResponse order = event.getOrder();

    log.info("Receive OrderCreatedEvent: orderId={}", order.getId());

    try {
      String content = buildEmailContent(order);

      SendNotificationRequest request = SendNotificationRequest.builder()
          .content(content)
          .to(order.getCustomerEmail())
          .channel(NotificationChannel.EMAIL.name())
          .build();

      notificationClient.sendNotification(request);

    } catch (Exception e) {
      log.error("Send notification failed for orderId={}", order.getId(), e);
    }
  }

  private String buildEmailContent(OrderResponse order) {

    String itemsText = order.getItems().stream()
        .map(i -> String.format("- %s x%d", i.getSku(), i.getQuantity()))
        .collect(Collectors.joining("\n"));

    return String.format(
        "Hi,\n\n" +
            "Your order has been created successfully.\n\n" +
            "Order ID: %d\n" +
            "Customer: %s\n" +
            "Status: %s\n\n" +
            "Items:\n%s\n\n" +
            "Discount: %d \n" +
            "Total: %d \n\n" +
            "Thank you for your purchase!",
        order.getId(),
        order.getCustomerEmail(),
        order.getStatus(),
        itemsText,
        order.getDiscountCents(),
        order.getTotalCents()
    );
  }

}

