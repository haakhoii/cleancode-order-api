package vn.r2s.training.api.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.dto.response.OrderItemsResponse;
import vn.r2s.training.api.dto.response.OrderResponse;
import vn.r2s.training.api.dto.response.PricingResponse;
import vn.r2s.training.api.entity.CustomerEntity;
import vn.r2s.training.api.entity.OrderEntity;
import vn.r2s.training.api.entity.OrderItemEntity;
import vn.r2s.training.api.enums.OrderStatus;
import vn.r2s.training.api.event.OrderCreatedEvent;
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
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public OrderResponse createOrder(CreateOrderRequest request) {

    CustomerEntity customer = getCustomerByEmail(request.getCustomerEmail());

    PricingResponse pricing = pricingService.calculate(request.getItems());

    int discount = discountService.calculate(
        pricing.getTotalCents(),
        request
    );
    if (discount < 0) {
      throw new BadRequestException("Invalid discount");
    }
    if (discount > pricing.getTotalCents()) {
      throw new BadRequestException("Discount exceeds total amount");
    }

    int finalTotal = pricing.getTotalCents() - discount;
    OrderEntity order = createAndSaveOrder(customer, pricing, finalTotal, discount);
    OrderResponse orderResponse = mapToResponse(order);
    eventPublisher.publishEvent(
        new OrderCreatedEvent(orderResponse)
    );

    return orderResponse;
  }

  private OrderEntity createAndSaveOrder(
      CustomerEntity customer,
      PricingResponse pricing,
      int finalTotal,
      int discount
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
            .build()
        )
        .toList();
  }
}