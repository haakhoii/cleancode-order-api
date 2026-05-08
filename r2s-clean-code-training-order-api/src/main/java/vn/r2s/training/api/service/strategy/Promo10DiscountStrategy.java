package vn.r2s.training.api.service.strategy;

import org.springframework.stereotype.Component;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.enums.DiscountCategory;
import vn.r2s.training.api.enums.DiscountCode;

@Component
public class Promo10DiscountStrategy implements DiscountStrategy {

  @Override
  public DiscountCategory category() {
    return DiscountCategory.PROMO;
  }

  @Override
  public boolean isApplicable(CreateOrderRequest request) {
    return request.getDiscountCode() == DiscountCode.PROMO10;
  }

  @Override
  public int calculate(int totalCents, CreateOrderRequest request) {
    return (int) (totalCents * 0.1);
  }
}
