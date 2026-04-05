package vn.r2s.training.api.dto.response;

import lombok.*;
import vn.r2s.training.api.entity.ProductEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingItemResponse {
  private ProductEntity product;
  private int quantity;
  private int unitPriceCents;
  private int lineTotalCents;
}
