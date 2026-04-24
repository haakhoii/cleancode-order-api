# Review R2 - ORDER SERVICE + NOTIFICATION SERVICE

---

## 1. Tổng quan hệ thống

Hệ thống gồm hai service giao tiếp với nhau:

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ORDER SERVICE                               │
│                                                                     │
│  POST /api/orders (create order)                                    │
│    → Tạo order và lưu vào db với status PENDING                     │
│    → gọi Notification Service qua OpenFeign                         │
└──────────────────────────────┬──────────────────────────────────────┘
                               │  HTTP POST /api/notifications/send
                               ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                      NOTIFICATION SERVICE                                 │
│                                                                           │
│  NotificationController.send()                                            │
│    → NotificationServiceImpl.sendOrderNotification()                      │
│        1. tạo notificationId (UUID)                                       │
│        2. lưu NotificationHistory → MongoDB  (status = PENDING)           │
│        3. NotificationProducer.send() → Kafka topic "order.notification"  │ 
│    → return NotificationHistory ngay lập tức                              │
│                                                                           │
│  [Kafka Consumer — chạy bất đồng bộ]                                      │
│    → đọc message từ topic "order.notification"                            │
│    → tìm record PENDING bằng notificationId                               │
│    → EmailSender / SmsSender → thực sự gửi thông báo                      │
│    → update status → SUCCESS / FAILED                                     │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Flow hoạt động chi tiết

### Bước 1 — Order Service tạo đơn hàng

```java
// OrderCommandServiceImpl.java
@Override
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
  CustomerEntity customer = getCustomerByEmail(request.getCustomerEmail());
  PricingResponse pricing = pricingService.calculate(request.getItems());
  int discount = discountService.calculate(pricing.getTotalCents(), request);
  validateDiscount(discount, pricing.getTotalCents());
  int finalTotal = pricing.getTotalCents() - discount;
  String verificationCode = generateVerificationCode();
  OrderEntity order = createAndSaveOrder(customer, pricing, finalTotal, discount);
  
  sendOrderNotification(order, verificationCode);

  return orderMapper.toResponse(order);
}
```

Đơn hàng được tạo và lưu vào PostgreSQL với `status = PENDING`. Sau đó Order Service gọi Notification Service để thông báo cho customer.

---

### Bước 2 — OpenFeign gọi sang Notification Service

```java
// NotificationClient.java (Order Service)
@FeignClient(name = "notification-service", url = "${notification.service.url}")
public interface NotificationClient {
    @PostMapping("/api/notifications/send")
    ResponseEntity<NotificationResponse> sendNotification(
        @RequestBody SendOrderNotificationRequest request
    );
}
```

OpenFeign tự động serialize request thành HTTP POST và gửi sang Notification Service. Order Service **không cần biết** bên trong Notification Service xử lý thế nào — đây là ranh giới rõ ràng giữa hai service.

---

### Bước 3 — Notification Service (Kafka) nhận request, lưu DB, publish Kafka

```java
@Override
public NotificationHistory sendOrderNotification(SendNotificationRequest request) {
  NotificationHistory history = NotificationHistory.builder()
      .id(UUID.randomUUID().toString())
      .orderId(request.getOrderId())
      .to(request.getTo())
      .subject(request.getSubject())
      .content(request.getContent())
      .verificationCode(request.getVerificationCode())
      .status(NotificationStatus.PENDING)
      .createdAt(Instant.now())
      .build();
  historyRepository.save(history);

  // producer
  publishNotificationEvent(request, notificationId);

  log.info("[NotificationService] Queued orderId=[{}] notificationId=[{}] email=[{}]",
      request.getOrderId(), notificationId, request.getTo());

  return history;
}
```

Điểm quan trọng nhất: **lưu MongoDB trước, publish Kafka sau, return ngay**. Order Service không bị block chờ email thực sự được gửi.

---

### Bước 4 — Kafka Consumer gửi thông báo thật và update status

```java
// NotificationConsumer.java (Notification Service)
@KafkaListener(topics = "order.notification", groupId = "notification-group")
public void consume(NotificationMessage message) {

    // Tìm record PENDING đã lưu ở bước 3
    NotificationHistory history = historyRepository
        .findById(message.getNotificationId())
        .orElseThrow();

    try {
        // Chọn đúng sender theo channel (EMAIL hoặc SMS)
        NotificationSender sender = senders.stream()
            .filter(s -> s.getType() == message.getChannel())
            .findFirst()
            .orElseThrow();

        sender.send(message);

        history.setStatus(NotificationStatus.SUCCESS);
    } catch (Exception e) {
        history.setStatus(NotificationStatus.FAILED);
        history.setErrorMessage(e.getMessage());
    }

    // Update record đã có — không tạo record mới
    historyRepository.save(history);
}
```

Consumer xử lý bất đồng bộ, hoàn toàn độc lập với HTTP request ban đầu.

---

## 3. Tại sao dùng OpenFeign + Kafka?

Đây là câu hỏi quan trọng nhất. Để hiểu lý do, cần xem điều gì xảy ra nếu chỉ dùng một trong hai.

### 3.1 Nếu chỉ dùng OpenFeign

Tức là Order Service gọi Notification Service qua HTTP, và Notification Service gọi thẳng lên server để gửi email trong cùng một request.

```
Order Service
    │
    │  HTTP POST (đợi...)
    ▼
Notification Service
    │
    │  Gửi email qua SMTP (chậm, có thể 1-3 giây)
    ▼
SMTP Server
    │
    │  Done (hoặc lỗi)
    ▼
Notification Service trả response
    │
    ▼
Order Service nhận response
    │
    ▼
Trả kết quả về user (sau 1-3 giây chờ SMTP)
```

**Vấn đề 1 — Blocking:** HTTP request của user phải chờ email gửi xong. SMTP thường mất 1-3 giây, thậm chí hơn. User trải nghiệm đăng đơn hàng bị chậm vì phải chờ email.

**Vấn đề 2 — Fragile:** SMTP server bị down hoặc chậm → Notification Service timeout → lỗi lan ngược về Order Service → đặt hàng thất bại. Đơn hàng fail chỉ vì email không gửi được là điều không thể chấp nhận trong production.

**Vấn đề 3 — Không có retry:** Gửi fail một lần là mất luôn. Không có cơ chế tự động gửi lại.

**Vấn đề 4 — Không có audit trail:** Không biết đã gửi bao nhiêu notification, trạng thái của từng cái là gì, cái nào fail và tại sao.

---

### 3.2 Nếu chỉ dùng Kafka

Tức là Order Service publish message trực tiếp lên Kafka, bỏ qua bước gọi HTTP đến Notification Service.

```
  Order Service
       │  Publish message → Kafka topic
       │  (không biết có ai nhận không)
       ▼
    [Kafka]
       │
       ▼
  Notification Consumer → gửi email
```

**Vấn đề 1 — Không có validation ngay lập tức:** Nếu request sai format (thiếu email, sai channel...), Order Service không biết ngay. Phải đợi đến khi consumer xử lý mới phát hiện — rất khó debug.

**Vấn đề 2 — Không có HTTP acknowledgment:** Order Service không nhận được `notificationId`, không biết notification đã được tạo hay chưa. Không có cách nào track sau này.

**Vấn đề 3 — Coupling ở tầng infrastructure:** Order Service phải biết tên Kafka topic, format message, schema của Notification Service. Đây là coupling ngầm rất nguy hiểm — nếu Notification Service đổi topic hoặc schema, Order Service phải đổi theo mà không có contract rõ ràng nào.

**Vấn đề 4 — Không có record trước khi Kafka xử lý:** Nếu Kafka broker crash trước khi consumer commit offset, message mất hoàn toàn. Không có gì để retry.

---

### 3.3 Tại sao phải kết hợp cả hai

Kết hợp OpenFeign + MongoDB + Kafka giải quyết tất cả vấn đề trên thông qua pattern **Accept & Defer** (Nhận ngay, xử lý sau):

```
Order Service
    │
    │  HTTP POST (đợi ~vài ms)
    ▼
Notification Service
    ├── 1. save MongoDB: status = PENDING   ← cam kết sẽ gửi, không mất dù Kafka crash
    └── 2. publish Kafka                    ← giao việc gửi thật cho consumer
    │
    │  return HTTP 200 ngay (không đợi email gửi xong)
    ▼
Order Service nhận response (có notificationId)
    │
    ▼
Trả kết quả về user (nhanh)

[Bất đồng bộ — sau đó]
    Kafka Consumer
        → gửi email thật
        → update MongoDB: PENDING → SUCCESS / FAILED
```

**OpenFeign lo 3 việc:**

Thứ nhất, **contract rõ ràng**. HTTP API là hợp đồng giữa hai service. Notification Service tự quyết định dùng Kafka hay gì bên trong — Order Service không cần biết và không phụ thuộc vào implementation đó.

Thứ hai, **validation ngay lập tức**. Request sai → HTTP 400 ngay lập tức. Order Service biết ngay để xử lý, không phải đợi đến khi consumer xử lý.

Thứ ba, **acknowledgment có giá trị**. Order Service nhận về `notificationId` — bằng chứng rằng notification đã được tạo và đang ở trạng thái PENDING.

**Kafka lo 2 việc:**

Thứ nhất, **async processing**. Việc gửi email/SMS thật sự diễn ra bất đồng bộ. Order Service không bị block. SMTP chậm hay nhanh không ảnh hưởng đến trải nghiệm đặt hàng của user.

Thứ hai, **retry tự động**. Kafka consumer fail → Kafka tự retry. Không cần viết thêm code retry thủ công.

---

### 3.4 Vai trò của MongoDB trong pattern này

```
OpenFeign nhận request
        │
        ▼
   save MongoDB PENDING   ← nếu Kafka crash sau đây, record vẫn còn
        │
        ▼
   publish Kafka          ← nếu thành công, consumer sẽ update record
        │
        ▼
   return HTTP 200        ← Order Service có notificationId
```

**Nếu không có bước save MongoDB PENDING:** Khi Kafka broker tạm thời down, notification request biến mất hoàn toàn — không có record nào, không có cách nào retry.

**Khi có save MongoDB PENDING:** Dù Kafka crash, record PENDING vẫn tồn tại trong DB. Một scheduled job có thể scan toàn bộ PENDING records và re-publish lên Kafka — đảm bảo notification cuối cùng sẽ được gửi.

**Câu hỏi đặt ra: dữ liệu lưu trong mongo db có bị xoá không:** toaàn bộ dữ liệu luưu trong mongo db sẽ không bị xoá, do mongo db được sử dụng như một ** design patter: audit log**  -> nếu như xảy ra lỗi, có thể dựa vào dữ liệu trong db để kiểm tra về các trạng thái, diễn ra vào lúc nào

---

### 3.5 Bảng so sánh

| Tiêu chí | Chỉ OpenFeign | Chỉ Kafka | OpenFeign + MongoDB + Kafka |
|---|:---:|:---:|:---:|
| Order Service bị block chờ gửi email? | Có | Không | Không |
| Contract rõ ràng giữa hai service? | Có | Không | Có |
| Validation request ngay lập tức? | Có | Không | Có |
| Có audit trail (lịch sử notification)? | Không | Không đầy đủ | Có (PENDING từ đầu) |
| Email fail → đặt hàng fail? | Có | Không | Không |
| Retry tự động khi gửi fail? | Không | Có | Có |
| Message mất khi Kafka crash? | Không áp dụng | Có thể | Không (PENDING trong DB) |

---

## 4. Tại sao Notification Service dùng MongoDB?
### 4.1 MongoDB là gì?
MongoDB là một **database lưu dữ liệu dạng document** (tài liệu), thay vì dạng bảng như PostgreSQL hay MySQL.

Dữ liệu trong MongoDB được lưu dưới dạng JSON, trông như thế này:

```json
{
  "notificationId": "0d91397a-e64c-40b5-803f-0af0c6876192",
  "orderId": "68",
  "to": "aakhoii207@gmail.com",
  "subject": "Order Verification Code",
  "content": "Your order has been created successfully.\n\nOrder ID: 68\nStatus: PENDING\nCreated At: 2026-04-21T11:22:31.098910400\n\nItems:\n\n- SKU-MAC x3 (32,000,000)\n\nTotal: 95,925,000\nDiscount: 75,000\nFinal Amount: 95,850,000\n\nVerification Code: 816DC088\n\nUse this code to confirm your order.\n\nThank you!",
  "verificationCode": "816DC088",
  "channel": "EMAIL"
}
```

### 4.2 Vấn đề nếu dùng PostgreSQL hoặc MySQL

`NotificationHistory` entity:

```java
@Document(collection = "notification_history")
public class NotificationHistory {
    @Id
    private String id;
    private String orderId;
    private String to;
    private String subject;
    private String content;
    private String verificationCode;
    private NotificationStatus status;
    private Instant createdAt;
}
```

Nhưng trong thực tế, mỗi kênh notification (EMAIL, SMS, ZALO...) cần lưu những thông tin hoàn toàn khác nhau:

```
EMAIL cần: subject, templateId, cc, bcc, attachments
SMS   cần: phoneNumber, provider, countryCode, messageId
PUSH  cần: deviceToken, appId, badgeCount, deeplink, ttl
Zalo  cần: zaloUserId, templateId, params (Map<String, Object>)
```

Nếu dùng PostgreSQL/MySQL, có 3 lựa chọn và cả 3 đều có vấn đề:

**Cách 1 — Thêm tất cả columns vào một bảng:**

```sql
CREATE TABLE notification_history (
    id VARCHAR PRIMARY KEY,
    order_id VARCHAR,
    customer_email VARCHAR,
    -- email fields
    subject VARCHAR,
    template_id VARCHAR,
    cc VARCHAR,
    -- sms fields
    phone_number VARCHAR,
    sms_provider VARCHAR,
    country_code VARCHAR,
    ...
)
```

Vấn đề: Mỗi row chỉ dùng một phần columns, phần còn lại là NULL. Thêm kênh mới phải ALTER TABLE — downtime hoặc migration phức tạp trong production.

**Cách 2 — Dùng column TEXT lưu JSON string:**

```sql
metadata TEXT  -- '{"subject":"...","templateId":"..."}'
```

Vấn đề: Không query được bên trong. Muốn tìm tất cả notification dùng templateId cụ thể phải dùng `LIKE '%templateId%'` — không chính xác, không thể index, performance rất tệ khi data lớn.

**Cách 3 — Tạo bảng riêng cho từng kênh:**

```sql
TABLE email_notification_history  (id, subject, template_id, ...)
TABLE sms_notification_history    (id, phone_number, provider, ...)
```

Vấn đề: Thêm kênh mới phải tạo bảng mới, sửa code nhiều chỗ. Query lịch sử tất cả notification của một order phải dùng UNION ALL nhiều bảng — phức tạp và chậm.

---

### 4.3 MongoDB là lựa chọn tự nhiên

MongoDB lưu mỗi document theo schema riêng. Thêm kênh mới hoàn toàn không cần đụng đến database:

```json
// EMAIL notification
{
  "_id": "uuid-1",
  "orderId": "1001",
  "channel": "EMAIL",
  "customerEmail": "john@example.com",
  "subject": "Order Verification Code",
  "verificationCode": "ABC12345",
  "status": "SUCCESS",
  "createdAt": "2024-01-01T10:00:00Z"
},

// SMS notification — thêm sau, không cần migration
{
  "_id": "uuid-2",
  "orderId": "1002",
  "channel": "SMS",
  "phoneNumber": "+84901234567",
  "provider": "Twilio",
  "verificationCode": "XYZ98765",
  "status": "PENDING",
  "createdAt": "2024-01-01T10:01:00Z"
}
```

Ngoài tính linh hoạt về schema, notification history còn có 3 đặc điểm phù hợp với MongoDB:

**Write-heavy, ít JOIN:** Notification history ghi rất nhiều (mỗi đơn hàng tạo ít nhất 1 record), nhưng khi đọc chủ yếu chỉ cần lấy theo `orderId` hoặc `recipient (customer email, phone numer, ...)` — không cần JOIN với bảng khác. Đây là use case MongoDB được tối ưu cho.

**Volume lớn, cần scale ngang:** Notification history tăng liên tục và không bao giờ giảm (chỉ lưu trữ). MongoDB sharding (phân mảnh) theo `customerId` hoặc `createdAt` cho phép scale ngang tự nhiên.

**ID là UUID dạng String:** MongoDB dùng `_id` dạng String rất tự nhiên — không cần sequence, không cần auto-increment, không có write contention khi insert song song nhiều records.

---

### 4.3 Bảng so sánh

| Tiêu chí | PostgreSQL / MySQL | MongoDB |
|---|---|---|
| Schema khác nhau mỗi channel | Khó — cần nhiều bảng hoặc nullable columns | Tự nhiên — mỗi document tự định nghĩa |
| Thêm channel mới | Cần migration, có thể downtime | Không cần đụng schema |
| Query cross-channel (lịch sử 1 order) | Phức tạp — UNION ALL | Đơn giản — query cùng 1 collection |
| JOIN với bảng khác | Mạnh | Không cần (data self-contained) |
| Write throughput cao | Được nhưng cần tuning | Tối ưu sẵn cho write-heavy |
| Scale ngang | Phức tạp, cần setup thủ công | Built-in sharding |
| UUID làm primary key | Được nhưng ảnh hưởng B-tree index | Tự nhiên với `_id` dạng String |
| Transaction phức tạp nhiều bảng | Mạnh hơn | Không cần ở use case này |

---

## 5. Tổng kết

Ba công cụ trong hệ thống này giải quyết ba vấn đề hoàn toàn khác nhau và không thứ nào thay thế được thứ nào:

```
OpenFeign → Contract rõ ràng + Validate request ngay tại thời điểm gọi + Có cơ chế acknowledgment (xác nhận phản hồi)

MongoDB → Lưu audit trail (lịch sử/trace log) + Đảm bảo không mất request (đóng vai trò buffer trạng thái PENDING)

Kafka → Xử lý bất đồng bộ (async) + Retry tự động khi lỗi + Không block Order Service
```

**OpenFeign không thể bị thay bằng Kafka trực tiếp** vì sẽ mất contract, mất validation ngay lập tức, và Order Service phải biết implementation nội bộ của Notification Service.

**Kafka không thể bị bỏ đi để chỉ dùng OpenFeign đồng bộ** vì Order Service sẽ bị block chờ SMTP, và email fail sẽ kéo theo đặt hàng fail.

**MongoDB không thể bị bỏ đi** vì nếu Kafka crash sau khi controller return HTTP 200, notification request mất hoàn toàn — không có gì để retry.

**MongoDB không phải là PostgreSQL** vì notification history có schema linh hoạt theo từng channel, write-heavy, không cần JOIN, và cần scale ngang khi data lớn — những thứ PostgreSQL không phải được tối ưu cho.

---

