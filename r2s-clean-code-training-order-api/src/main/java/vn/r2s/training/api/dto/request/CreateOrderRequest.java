package vn.r2s.training.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.r2s.training.api.enums.CustomerType;
import vn.r2s.training.api.enums.DiscountCode;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

  @Email
  @NotBlank
  private String customerEmail;

  private CustomerType customerType;

  @NotEmpty
  @Valid
  private List<OrderItemRequest> items;

  private DiscountCode discountCode;
}