package vn.r2s.training.api.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.service.DiscountCalculationService;
import vn.r2s.training.api.service.strategy.DiscountStrategy;

@Service
@RequiredArgsConstructor
public class DiscountCalculationServiceImpl implements DiscountCalculationService {

  private final List<DiscountStrategy> strategies;

  @Override
  public int calculate(int totalCents, CreateOrderRequest request) {
    return strategies.stream()
        .filter(s -> s.isApplicable(request))
        .mapToInt(s -> s.calculate(totalCents, request))
        .sum();
  }
}
