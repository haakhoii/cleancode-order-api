# Checklist refactor theo thứ tự triển khai (P0 → P2)

## P0 — Rủi ro bảo mật (security + correctness)
- **Rotate secrets ngay lập tức**: SMTP Gmail (đang lộ), Mongo credential, Postgres password.
- **Xoá secrets khỏi repo**:
  - Chuyển `spring.datasource.*`, `spring.mail.*`, `spring.data.mongodb.uri`, `kafka.bootstrap-servers` sang **ENV**.
  - Commit kèm **`.env.example`** (không chứa giá trị thật).
- **Cập nhật `SecurityConfig`**:
  - Mặc định `denyAll()`/`authenticated()` cho mọi endpoint, chỉ `permitAll()` cho `swagger` (chỉ bật ở dev) và endpoint thật sự public.
- **Fix verify order flow**:
  - Lưu `verificationCodeHash`, `expiresAt`, `attempts`, `verifiedAt` (hoặc bảng riêng).
  - `verifyOrder(orderId, code)` phải so khớp hash + kiểm tra hạn/attempts + idempotent.
- **Không log dữ liệu nhạy cảm**:
  - Không log `verificationCode` (ở `OrderCommandServiceImpl`, `NotificationProducer`).
  - `ApiExceptionHandler` không trả `ex.getMessage()` cho lỗi 500.

## P1 — Ổn định vận hành & tích hợp (resilience + error contract)
- **Chuẩn hoá error response** (1 format duy nhất):
  - Handler cho: validation body, constraint violation, JSON parse, Feign error, DB constraint.
  - Trả `errorCode`, `message`, `details`, `traceId`.
- **Tách sync call khỏi business critical path**:
  - Ưu tiên **event-driven** (Kafka/outbox) cho notification; tránh Feign sync khi tạo order.
  - Nếu dùng Feign: thêm timeout, retry/backoff, circuit breaker, phân loại 4xx/5xx.
- **Kafka hardening**:
  - Bỏ `spring.json.trusted.packages: "*"` → whitelist package cụ thể.
  - Topic/group-id đưa vào config, tránh hardcode.
- **Repo hygiene**:
  - Loại `target/` khỏi VCS bằng `.gitignore` chuẩn Maven.

## P2 — Nâng chất lượng code & observability
- **Discount strategies**: bỏ `instanceof` phân nhóm; chuyển sang metadata (category/priority) hoặc registry.
- **Observability**:
  - Thêm Actuator (health/readiness/liveness), metrics, tracing, structured logging + correlation-id.
- **Profiles dev/prod**:
  - Swagger chỉ bật ở dev, log level debug chỉ dev.

