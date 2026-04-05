package vn.r2s.training.api.service.strategy;

import org.springframework.stereotype.Component;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.enums.CustomerType;

@Component
public class NewCustomerDiscountStrategy implements DiscountStrategy {

  @Override
  public boolean isApplicable(CreateOrderRequest request) {
    return request.getCustomerType() == CustomerType.VIP;
  }

  @Override
  public int calculate(int totalCents, CreateOrderRequest request) {
    return 20000;
  }
}
