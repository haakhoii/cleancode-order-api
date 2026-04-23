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
import vn.r2s.training.api.dto.response.OrderResponse;
import vn.r2s.training.api.dto.response.PricingResponse;
import vn.r2s.training.api.entity.CustomerEntity;
import vn.r2s.training.api.entity.OrderEntity;
import vn.r2s.training.api.entity.OrderItemEntity;
import vn.r2s.training.api.enums.NotificationChannel;
import vn.r2s.training.api.enums.OrderStatus;
import vn.r2s.training.api.exception.BadRequestException;
import vn.r2s.training.api.exception.NotFoundException;
import vn.r2s.training.api.mapper.OrderMapper;
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
  private final OrderMapper orderMapper;

  @Override
  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {
    CustomerEntity customer = getCustomerByEmail(request.getCustomerEmail());
    PricingResponse pricing = pricingService.calculate(request.getItems());

    int discount = discountService.calculate(pricing.getTotalCents(), request);
    validateDiscount(discount, pricing.getTotalCents());

    int finalTotal = pricing.getTotalCents() - discount;
    String verificationCode = generateVerificationCode();

    OrderEntity order = createAndSaveOrder(customer, pricing, finalTotal, discount);
    sendOrderNotification(order, verificationCode);

    return orderMapper.toResponse(order);
  }

  @Override
  @Transactional
  public String verifyOrder(Long orderId, String code) {
    OrderEntity order = orderRepository.findById(orderId)
        .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

    validateOrderCanBeVerified(order);

    order.setStatus(OrderStatus.SUCCESS);
    orderRepository.save(order);
    log.info("[verifyOrder] Order [{}] verified successfully → status=SUCCESS", orderId);

    return "Order verified successfully with orderId: " + orderId;
  }

  // private method
  private void validateDiscount(int discount, int totalCents) {
    if (discount < 0) {
      throw new BadRequestException("Invalid discount");
    }
    if (discount > totalCents) {
      throw new BadRequestException("Discount exceeds total amount");
    }
  }

  private void validateOrderCanBeVerified(OrderEntity order) {
    if (order.getStatus() == OrderStatus.SUCCESS) {
      throw new BadRequestException("Order already verified");
    }
    if (order.getStatus() == OrderStatus.CANCELLED) {
      throw new BadRequestException("Order is cancelled");
    }
  }

  private static String generateVerificationCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
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

    List<OrderItemEntity> items = buildOrderItems(order, pricing);
    order.setItems(items);
    return orderRepository.save(order);
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

  private void sendOrderNotification(OrderEntity order, String verificationCode) {
    try {
      SendOrderNotificationRequest notificationRequest = buildNotificationRequest(order, verificationCode);
      notificationClient.sendNotification(notificationRequest);
      log.info("[createOrder] Sent verification code=[{}] for orderId=[{}]", verificationCode, order.getId());
    } catch (Exception e) {
      log.error("[createOrder] Failed to send notification for orderId=[{}]: {}", order.getId(), e.getMessage(), e);
    }
  }

  private SendOrderNotificationRequest buildNotificationRequest(OrderEntity order, String verificationCode) {
    return SendOrderNotificationRequest.builder()
        .orderId(String.valueOf(order.getId()))
        .to(order.getCustomer().getEmail())
        .subject("Order Verification Code")
        .content(buildNotificationContent(order, verificationCode))
        .verificationCode(verificationCode)
        .channel(NotificationChannel.SMS)
        .build();
  }

  private String buildNotificationContent(OrderEntity order, String verificationCode) {
    String itemsText = order.getItems().stream()
        .map(i -> String.format("- %s x%d (%,d)",
            i.getProduct().getSku(),
            i.getQuantity(),
            i.getUnitPriceCents()))
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

  private CustomerEntity getCustomerByEmail(String email) {
    return customerRepository.findByEmail(email)
        .orElseThrow(() -> new NotFoundException("Customer not found: " + email));
  }
}