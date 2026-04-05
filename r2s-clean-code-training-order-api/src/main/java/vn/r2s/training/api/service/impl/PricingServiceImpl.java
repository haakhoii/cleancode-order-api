package vn.r2s.training.api.service.impl;

import java.util.ArrayList;
import java.util.List;
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

    List<PricingItemResponse> result = new ArrayList<>();
    int total = 0;

    for (OrderItemRequest item : items) {

      ProductEntity product = productRepository.findBySku(item.getSku())
          .orElseThrow(() -> new RuntimeException("Product not found: " + item.getSku()));

      int unitPrice = product.getPriceCents();
      int lineTotal = unitPrice * item.getQuantity();

      result.add(PricingItemResponse.builder()
          .product(product)
          .quantity(item.getQuantity())
          .unitPriceCents(unitPrice)
          .lineTotalCents(lineTotal)
          .build());

      total += lineTotal;
    }

    return PricingResponse.builder()
        .totalCents(total)
        .items(result)
        .build();
  }
}
