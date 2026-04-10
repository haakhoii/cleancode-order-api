package vn.r2s.training.api.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.service.DiscountCalculationService;
import vn.r2s.training.api.service.strategy.DiscountStrategy;
import vn.r2s.training.api.service.strategy.NewCustomerDiscountStrategy;
import vn.r2s.training.api.service.strategy.*;

@Service
@RequiredArgsConstructor
public class DiscountCalculationServiceImpl implements DiscountCalculationService {

  private final List<DiscountStrategy> strategies;

  @Override
  public int calculate(int totalCents, CreateOrderRequest request) {

    int customerDiscount = strategies.stream()
        .filter(this::isCustomerStrategy)
        .filter(s -> s.isApplicable(request))
        .mapToInt(s -> s.calculate(totalCents, request))
        .max()
        .orElse(0);

    int promoDiscount = strategies.stream()
        .filter(this::isPromoStrategy)
        .filter(s -> s.isApplicable(request))
        .mapToInt(s -> s.calculate(totalCents, request))
        .max()
        .orElse(0);

    return customerDiscount + promoDiscount;
  }

  private boolean isCustomerStrategy(DiscountStrategy strategy) {
    return strategy instanceof VipCustomerDiscountStrategy
        || strategy instanceof NewCustomerDiscountStrategy;
  }

  private boolean isPromoStrategy(DiscountStrategy strategy) {
    return strategy instanceof Promo10DiscountStrategy
        || strategy instanceof Promo20DiscountStrategy
        || strategy instanceof FreeShipDiscountStrategy;
  }
}
