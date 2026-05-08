package vn.r2s.training.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_verifications")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "order_id", nullable = false, unique = true)
  private OrderEntity order;

  @Column(name = "verification_code_hash", nullable = false)
  private String verificationCodeHash;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "is_verified", nullable = false)
  @Builder.Default
  private boolean verified = false;

  @Column(name = "verified_at")
  private LocalDateTime verifiedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}