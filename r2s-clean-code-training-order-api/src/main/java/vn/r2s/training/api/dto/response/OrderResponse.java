package vn.r2s.training.api.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
  private Long id;
  private String customerEmail;
  private String status;
  private int totalCents;
  private int discountCents;

  private List<OrderItemsResponse> items;
}
