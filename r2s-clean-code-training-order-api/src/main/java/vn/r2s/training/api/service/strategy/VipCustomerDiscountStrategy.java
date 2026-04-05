package vn.r2s.training.api.service.strategy;

import org.springframework.stereotype.Component;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.enums.CustomerType;
import vn.r2s.training.api.enums.DiscountCode;

@Component
public class VipCustomerDiscountStrategy implements DiscountStrategy {

  @Override
  public boolean isApplicable(CreateOrderRequest request) {
    return request.getCustomerType() == CustomerType.VIP;
  }

  @Override
  public int calculate(int totalCents, CreateOrderRequest request) {
    return (int) (totalCents * 0.05);
  }
}
