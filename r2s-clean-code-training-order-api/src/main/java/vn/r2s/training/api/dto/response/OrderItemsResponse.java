package vn.r2s.training.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemsResponse {
  private String sku;
  private int quantity;
  private int unitPriceCents;
  private int lineTotalCents;
}
