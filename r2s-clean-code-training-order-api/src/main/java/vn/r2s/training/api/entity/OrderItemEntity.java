package vn.r2s.training.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id")
  private OrderEntity order;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private ProductEntity product;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price_cents", nullable = false)
  private int unitPriceCents;

  @Column(name = "line_total_cents", nullable = false)
  private int lineTotalCents;
}
