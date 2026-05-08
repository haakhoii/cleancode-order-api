package vn.r2s.training.api.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
import vn.r2s.training.api.entity.OrderVerification;
import vn.r2s.training.api.enums.NotificationChannel;
import vn.r2s.training.api.enums.OrderStatus;
import vn.r2s.training.api.exception.BadRequestException;
import vn.r2s.training.api.exception.NotFoundException;
import vn.r2s.training.api.mapper.OrderMapper;
import vn.r2s.training.api.repository.CustomerRepository;
import vn.r2s.training.api.repository.OrderRepository;
import vn.r2s.training.api.repository.OrderVerificationRepository;
import vn.r2s.training.api.service.DiscountCalculationService;
import vn.r2s.training.api.service.OrderCommandService;
import vn.r2s.training.api.service.PricingService;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCommandServiceImpl implements OrderCommandService {

  private static final int CODE_EXPIRY_MINUTES = 15;

  private final PricingService pricingService;
  private final DiscountCalculationService discountService;
  private final CustomerRepository customerRepository;
  private final OrderRepository orderRepository;
  private final OrderVerificationRepository verificationRepository;
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
    saveVerification(order, verificationCode);

    log.info("[createOrder] Order created orderId=[{}] customerId=[{}]",
        order.getId(), customer.getId());

    sendOrderNotification(order, verificationCode);
    return orderMapper.toResponse(order);
  }

  @Override
  @Transactional
  public String verifyOrder(Long orderId, String code) {
    OrderVerification verification = verificationRepository.findByOrderId(orderId)
        .orElseThrow(() -> new NotFoundException("Verification not found for order: " + orderId));
    if (verification.isVerified()) {
      log.info("[verifyOrder] Order [{}] already verified at {}", orderId, verification.getVerifiedAt());
      return "Order already verified successfully";
    }

    validateVerification(verification, code);
    verification.setVerified(true);
    verification.setVerifiedAt(LocalDateTime.now());
    verificationRepository.save(verification);
    OrderEntity order = verification.getOrder();
    order.setStatus(OrderStatus.SUCCESS);
    orderRepository.save(order);

    log.info("[verifyOrder] Order [{}] verified successfully → status=SUCCESS", orderId);
    return "Order verified successfully with orderId: " + orderId;
  }

  // private
  private void validateDiscount(int discount, int totalCents) {
    if (discount < 0) throw new BadRequestException("Invalid discount");
    if (discount > totalCents) throw new BadRequestException("Discount exceeds total amount");
  }

  private void validateVerification(OrderVerification verification, String code) {
    OrderEntity order = verification.getOrder();

    if (order.getStatus() == OrderStatus.CANCELLED) {
      throw new BadRequestException("Order is cancelled");
    }
    if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
      throw new BadRequestException("Verification code has expired");
    }

    String inputHash = sha256(code);
    if (!MessageDigest.isEqual(
        inputHash.getBytes(StandardCharsets.UTF_8),
        verification.getVerificationCodeHash().getBytes(StandardCharsets.UTF_8))) {
      throw new BadRequestException("Invalid verification code");
    }
  }

  private void saveVerification(OrderEntity order, String verificationCode) {
    LocalDateTime now = LocalDateTime.now();
    OrderVerification verification = OrderVerification.builder()
        .order(order)
        .verificationCodeHash(sha256(verificationCode))
        .expiresAt(now.plusMinutes(CODE_EXPIRY_MINUTES))
        .verified(false)
        .createdAt(now)
        .build();
    verificationRepository.save(verification);
  }

  private OrderEntity createAndSaveOrder(
      CustomerEntity customer, PricingResponse pricing,
      int finalTotal, int discount
  ) {
    LocalDateTime now = LocalDateTime.now();
    OrderEntity order = OrderEntity.builder()
        .customer(customer)
        .status(OrderStatus.PENDING)
        .totalCents(finalTotal)
        .discountCents(discount)
        .createdAt(now)
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
      notificationClient.sendNotification(buildNotificationRequest(order, verificationCode));
      log.info("[createOrder] Notification dispatched for orderId=[{}]", order.getId());
    } catch (Exception e) {
      log.error("[createOrder] Failed to dispatch notification for orderId=[{}]: {}",
          order.getId(), e.getMessage());
    }
  }

  private SendOrderNotificationRequest buildNotificationRequest(
      OrderEntity order, String verificationCode
  ) {
    return SendOrderNotificationRequest.builder()
        .orderId(String.valueOf(order.getId()))
        .to(order.getCustomer().getEmail())
        .subject("Order Verification Code")
        .content(buildNotificationContent(order, verificationCode))
        .verificationCode(verificationCode)
        .channel(NotificationChannel.EMAIL)
        .build();
  }

  private String buildNotificationContent(OrderEntity order, String verificationCode) {
    String itemsText = order.getItems().stream()
        .map(i -> String.format("- %s x%d (%,d)",
            i.getProduct().getSku(), i.getQuantity(), i.getUnitPriceCents()))
        .reduce("", (a, b) -> a + "\n" + b);

    return String.format(
        "Your order has been created successfully.\n\n"
            + "Order ID: %d\nStatus: %s\nCreated At: %s\n\n"
            + "Items:\n%s\n\n"
            + "Total: %,d\nDiscount: %,d\nFinal Amount: %,d\n\n"
            + "Verification Code: %s\n"
            + "This code expires in %d minutes.\n\nThank you!",
        order.getId(), order.getStatus(), order.getCreatedAt(),
        itemsText,
        order.getTotalCents(), order.getDiscountCents(),
        order.getTotalCents() - order.getDiscountCents(),
        verificationCode, CODE_EXPIRY_MINUTES
    );
  }

  private static String generateVerificationCode() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private CustomerEntity getCustomerByEmail(String email) {
    return customerRepository.findByEmail(email)
        .orElseThrow(() -> new NotFoundException("Customer not found: " + email));
  }
}