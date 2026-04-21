package vn.r2s.training.api.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.r2s.training.api.client.NotificationClient;
import vn.r2s.training.api.client.SendOrderNotificationRequest;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.dto.response.OrderItemsResponse;
import vn.r2s.training.api.dto.response.OrderResponse;
import vn.r2s.training.api.dto.response.PricingResponse;
import vn.r2s.training.api.entity.CustomerEntity;
import vn.r2s.training.api.entity.OrderEntity;
import vn.r2s.training.api.entity.OrderItemEntity;
import vn.r2s.training.api.enums.NotificationChannel;
import vn.r2s.training.api.enums.OrderStatus;
import vn.r2s.training.api.exception.BadRequestException;
import vn.r2s.training.api.exception.NotFoundException;
import vn.r2s.training.api.repository.CustomerRepository;
import vn.r2s.training.api.repository.OrderRepository;
import vn.r2s.training.api.service.DiscountCalculationService;
import vn.r2s.training.api.service.OrderCommandService;
import vn.r2s.training.api.service.PricingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandServiceImpl implements OrderCommandService {

  private final PricingService pricingService;
  private final DiscountCalculationService discountService;
  private final CustomerRepository customerRepository;
  private final OrderRepository orderRepository;
  private final NotificationClient notificationClient;

  @Override
  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {
    CustomerEntity customer = getCustomerByEmail(request.getCustomerEmail());
    PricingResponse pricing = pricingService.calculate(request.getItems());

    int discount = discountService.calculate(pricing.getTotalCents(), request);
    if (discount < 0) {
      throw new BadRequestException("Invalid discount");
    }
    if (discount > pricing.getTotalCents()) {
      throw new BadRequestException("Discount exceeds total amount");
    }
    int finalTotal = pricing.getTotalCents() - discount;
    String verificationCode = createVerifyCode();
    OrderEntity order = createAndSaveOrder(customer, pricing, finalTotal, discount);
    OrderResponse orderResponse = mapToResponse(order);
    sendOrderNotification(order, verificationCode);

    return orderResponse;
  }

  @Override
  @Transactional
  public String verifyOrder(Long orderId, String code) {
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

    if (order.getStatus() == OrderStatus.SUCCESS) {
      throw new BadRequestException("Order already verified");
    }
    if (order.getStatus() == OrderStatus.CANCELLED) {
      throw new BadRequestException("Order is cancelled");
    }
    order.setStatus(OrderStatus.SUCCESS);
    orderRepository.save(order);
    log.info("[verifyOrder] Order [{}] verified successfully → status=SUCCESS", orderId);

    return "Order verified successfully with orderId: " + orderId;
  }

  // private
  private static String createVerifyCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }

  private void sendOrderNotification(OrderEntity order, String verificationCode) {
    try {
      String content = createContent(order, verificationCode);

      SendOrderNotificationRequest notificationRequest = SendOrderNotificationRequest.builder()
          .orderId(String.valueOf(order.getId()))
          .to(order.getCustomer().getEmail())
          .subject("Order Verification Code")
          .content(content)
          .verificationCode(verificationCode)
          .channel(NotificationChannel.EMAIL)
          .build();

      notificationClient.sendNotification(notificationRequest);
      log.info("[createOrder] Sent verification code=[{}] for orderId=[{}]", verificationCode,
          order.getId());
    } catch (Exception e) {
      log.error("[createOrder] Failed to send notification for orderId=[{}]: {}", order.getId(),
          e.getMessage(), e);
    }
  }

  private static String createContent(OrderEntity order, String verificationCode) {
    String itemsText = order.getItems().stream()
        .map(i -> String.format("- %s x%d (%,d)",
            i.getProduct().getSku(),
            i.getQuantity(),
            i.getUnitPriceCents()
        ))
        .reduce("", (a, b) -> a + "\n" + b);

      return String.format(
        "Your order has been created successfully.\n\n" +
            "Order ID: %d\n" +
            "Status: %s\n" +
            "Created At: %s\n\n" +
            "Items:\n%s\n\n" +
            "Total: %,d\n" +
            "Discount: %,d\n" +
            "Final Amount: %,d\n\n" +
            "Verification Code: %s\n\n" +
            "Use this code to confirm your order.\n\n" +

            "Thank you!",
        order.getId(),
        order.getStatus(),
        order.getCreatedAt(),
        itemsText,
        order.getTotalCents(),
        order.getDiscountCents(),
        order.getTotalCents() - order.getDiscountCents(),
        verificationCode
    );
  }

  private OrderEntity createAndSaveOrder(
      CustomerEntity customer, PricingResponse pricing, int finalTotal, int discount
  ) {
    OrderEntity order = OrderEntity.builder()
        .customer(customer)
        .status(OrderStatus.PENDING)
        .totalCents(finalTotal)
        .discountCents(discount)
        .createdAt(LocalDateTime.now())
        .build();

    List<OrderItemEntity> orderItems = buildOrderItems(order, pricing);
    order.setItems(orderItems);
    return orderRepository.save(order);
  }

  private OrderResponse mapToResponse(OrderEntity order) {
    List<OrderItemsResponse> items = order.getItems().stream()
        .map(item -> OrderItemsResponse.builder()
            .sku(item.getProduct().getSku())
            .quantity(item.getQuantity())
            .unitPriceCents(item.getUnitPriceCents())
            .lineTotalCents(item.getLineTotalCents())
            .build())
        .toList();

    return OrderResponse.builder()
        .id(order.getId())
        .customerEmail(order.getCustomer().getEmail())
        .status(order.getStatus().name())
        .totalCents(order.getTotalCents())
        .discountCents(order.getDiscountCents())
        .items(items)
        .build();
  }

  private CustomerEntity getCustomerByEmail(String email) {
    return customerRepository.findByEmail(email)
        .orElseThrow(() -> new NotFoundException("Customer not found: " + email));
  }

  private List<OrderItemEntity> buildOrderItems(OrderEntity order, PricingResponse pricing) {
    return pricing.getItems().stream()
        .map(item -> OrderItemEntity.builder()
            .order(order)
            .product(item.getProduct())
            .quantity(item.getQuantity())
            .unitPriceCents(item.getUnitPriceCents())
            .lineTotalCents(item.getLineTotalCents())
            .build())
        .toList();
  }
}