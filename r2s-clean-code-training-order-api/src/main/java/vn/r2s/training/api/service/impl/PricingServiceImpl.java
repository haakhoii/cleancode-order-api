package vn.r2s.training.api.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.dto.request.OrderItemRequest;
import vn.r2s.training.api.dto.response.PricingItemResponse;
import vn.r2s.training.api.dto.response.PricingResponse;
import vn.r2s.training.api.entity.ProductEntity;
import vn.r2s.training.api.repository.ProductRepository;
import vn.r2s.training.api.service.PricingService;
@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

  private final ProductRepository productRepository;

  @Override
  public PricingResponse calculate(List<OrderItemRequest> items) {
    validate(items);

    Map<String, ProductEntity> productMap = loadProducts(items);

    List<PricingItemResponse> pricingItems = buildPricingItems(items, productMap);

    int total = calculateTotal(pricingItems);

    return toResponse(pricingItems, total);
  }

  // private
  private void validate(List<OrderItemRequest> items) {
    if (items == null || items.isEmpty()) {
      throw new IllegalArgumentException("Items must not be empty");
    }

    for (OrderItemRequest item : items) {
      if (item == null) {
        throw new IllegalArgumentException("Item must not be null");
      }
      if (item.getSku() == null || item.getSku().isBlank()) {
        throw new IllegalArgumentException("SKU must not be blank");
      }
      if (item.getQuantity() <= 0) {
        throw new IllegalArgumentException("Quantity must be greater than 0");
      }
    }
  }

  private Map<String, ProductEntity> loadProducts(List<OrderItemRequest> items) {

    List<String> skus = items.stream()
        .map(OrderItemRequest::getSku)
        .distinct()
        .toList();

    List<ProductEntity> products = productRepository.findBySkuIn(skus);

    Map<String, ProductEntity> productMap = products.stream()
        .collect(Collectors.toMap(ProductEntity::getSku, p -> p));

    for (String sku : skus) {
      if (!productMap.containsKey(sku)) {
        throw new RuntimeException("Product not found: " + sku);
      }
    }

    return productMap;
  }

  private List<PricingItemResponse> buildPricingItems(
      List<OrderItemRequest> items,
      Map<String, ProductEntity> productMap) {

    List<PricingItemResponse> pricingItems = new ArrayList<>();

    for (OrderItemRequest item : items) {
      ProductEntity product = productMap.get(item.getSku());

      int unitPrice = product.getPriceCents();
      int lineTotal = unitPrice * item.getQuantity();

      pricingItems.add(PricingItemResponse.builder()
          .product(product)
          .quantity(item.getQuantity())
          .unitPriceCents(unitPrice)
          .lineTotalCents(lineTotal)
          .build());
    }

    return pricingItems;
  }

  // ================= CALCULATE TOTAL =================
  private int calculateTotal(List<PricingItemResponse> items) {
    return items.stream()
        .mapToInt(PricingItemResponse::getLineTotalCents)
        .sum();
  }

  // ================= BUILD RESPONSE =================
  private PricingResponse toResponse(List<PricingItemResponse> items, int total) {
    return PricingResponse.builder()
        .items(items)
        .totalCents(total)
        .build();
  }
}
