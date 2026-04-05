package vn.r2s.training.api.dto.response;

import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingResponse {
  private int totalCents;
  private List<PricingItemResponse> items;
}
