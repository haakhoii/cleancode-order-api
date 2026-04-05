# OrderApi - Refactor Notes

## Overview

- Project: Order Api
- Tech stack:
  - Java 17
  - Spring boot 3.5.10
  - PostgresSQL
  - Flyway
  - Spring web
  - Lombok
  - Swagger
- Infrastructure:

```
r2s-clean-code-training-order-api
│
├── src
│   └── main
│       ├── java
│       │   └── vn
│       │       └── r2s
│       │           └── training
│       │               └── api
│       │                   ├── config
│       │                   ├── constants
│       │                   ├── controller
│       │                   ├── domain
│       │                   ├── dto
│       │                   ├── entity
│       │                   ├── exception
│       │                   ├── repository
│       │                   ├── service
│       │                   ├── util
│       │                   └── OrderApiApplication.java
│       │
│       └── resources
│           ├── application.yml
│           └── db
│               └── migration
│                   ├── V1__create_tables.sql
│                   └── V2__seed_data.sql
│
├── docker-compose.yml
└── pom.xml
```

---

## DAY 1: ĐỌC CODE & PHÂN TÍCH VẤN ĐỀ

## Mục tiêu:

- Hiểu business flow của Order API
- Nhận diện các vấn đề thiết kế & clean code

## Công việc cần làm:

### 1. Clone repo và chạy thành công: `docker compose up -d`
### 2. Đọc kỹ các file sau:

- OrderServiceImpl: bao gồm các chức năng
  - validate request
  - check customer
  - build order
  - build order item
  - check product
  - calculate total & discount
  - save db
  - send email
  - mapping response
- OrderService: tầng service được controller gọi đến thông qua interface, tránh gọi trực tiếp
- OrderController: Export API
  - Create order:
    - Method: POST
    - api: http://localhost:8080/api/orders
  - Get order by id:
    - Method: GET
    - api: http://localhost:8080/api/orders/{id}
- DTO & Entity:
  - Magic string -> hard code
  - Import dependency `lombok` để sử dụng các annotation có sẵn cho việc tạo constructor
- Discount logic trong createOrder:
  - sử dụng quá nhiều if-else
  - tính toán nằm trong createOrder
  - logic discount không rõ ràng

### 3. Input/ Output:
- Input:
  ```
  {
    "customerEmail": "alice@r2s.vn",
    "customerType": "VIP",
    "items": [
      {
        "sku": "SKU-IPHONE",
        "quantity": 10
      }
    ],
    "discountCode": "FREESHIP"
  }
  ```
- Output:
   ```
   {
     "id": 8,
     "customerEmail": "alice@r2s.vn",
     "status": "NEW",
     "totalCents": 249925000,
     "discountCents": 75000,
     "items": [
       {
         "sku": "SKU-IPHONE",
         "quantity": 10,
         "unitPriceCents": 25000000,
         "lineTotalCents": 250000000
       }
     ]
   }
   ```

### 4. Tạo file: docs/refactor-notes.md
### 5. Những vấn đề về source code
#### *SOLID*:

- SRP (Single Responsibility Principle):
  - Problem: Method createOrder() đang xử lý quá nhiều tác vụ -> God Method
    - validation request
    - load customer
    - build order entity
    - build order item
    - check product
    - calculate total
    - calculate discount
    - save database
    - send email
    - mapping response
    ```
      if (request == null) ...
    
      CustomerEntity customer = customerRepo.findByEmail() ...
           
      OrderEntity order = new OrderEntity();
           
      List<OrderItemEntity> orderItems = new ArrayList<>();
           
      for (OrderItemRequest itemReq : request.items) ...
           
      ProductEntity product = productRepo.findBySku() ...
           
      OrderItemEntity item = new OrderItemEntity();
           
      sendEmail(customer.getEmail(), "Thanks VIP! Your order id = " + saved.getId());
     ```
  - Impact:
    - Khó test/ maintain: mock nhiều dependency (CustomerRepository, ProductRepository, OrderRepository)
    - Không thể viết unit test theo kiểu từng logic
    - Khi thay đổi discount sẽ ảnh hưởng đến flow của createOrder
    - Hàm createOrder quá dài -> khó đọc
    - Việc viết chung các hàm như discount, validate ở trong createOrder không thể tái sử dụng code ở những chỗ khác
    - ```
      customerRepo.findByEmail()
      productRepo.findBySku()
      orderRepo.save()
      if (!StringUtil.isBlank(code)) { ... }
      ```
  - Refactor direction:
    - Tách các logic trong OrderService thành các interface/component riêng theo từng responsibility (OrderItemService, DiscountService, NotificationService, ...)
    - Mỗi class đảm nhiệm 1 chức năng duy nhất
    - OrderService là nơi điều phối các vai trò để xử lý

---

- OCP (Open/Closed Principle):
  - Problem: sử dụng quá nhiều if-else
    ```
       if ("PROMO10".equalsIgnoreCase(code)) { ... }
       else if ("PROMO20".equalsIgnoreCase(code)) { ... }
       else if ("FREESHIP".equalsIgnoreCase(code)) { ... }
    
       if (!StringUtil.isBlank(request.customerType)) { ... }
    ```
  - Impact:
    - Logic hiện tại đang hard code -> không mở rộng
    - Khi có thêm "PROMO30" -> phải sửa lại code hiện tại
  - Refactor:
    - Không sử dụng if-else -> Map/Registry
    - Tách logic discount thành DiscountService, DiscountStrategy
    - Mỗi discount là 1 hàm riêng Promo(X)Discount (X là giá trị phần trăm giảm)
    - Khi thêm mới logic (PROMO30) thì chỉ cần tạo class Promo(X)Discount -> không sửa logic code
    - ```
      interface DiscountStrategy { getCode() + apply(Order) }
    - ```
      class DiscountService { Map + lookup }
      ```
---

- DIP (Dependency Inversion Principle):
  - Problem:
    - OrderService đang phụ thuộc trực tiếp vào side-effect:
    - Business logic không được thông qua interface/port
      ```
        customerRepo.findByEmail()
        productRepo.findBySku()
        orderRepo.save()
        sendEmail(...)
        log.warn("Unknown discount code: {}", code);
        ...
      ```
  - Impact:
    - High Coupling -> việc tạo order liên quan đến gửi email
    - Tạo order thành công nhưng email bị fail -> ảnh hưởng đến flow của order
    - Không thể mở rộng thêm gửi bằng SMS/ Queue
  - Refactor:
    - Tạo abstraction ```interface NotificationService { send(...) }```
    - Implement EmailNotification ```EmailNotificationService implements NotificationService```
    - Khi mở rộng (chuyển từ email sang SMS) ```SMSNotificationService implements NotificationService```
    - OrderService gọi thông qua abstract
    - Tách biệt các business logic ra khỏi side-effect

---

- ISP (Interface Segregation Principle):
  - Problem: chứa những method không liên quan đến business logic của Order
     ```java
     void sendEmail(String to, String content);
  
     String exportOrderAsCsv(Long id);
     ```
  - Impact:
    - Import những method mà không dùng tới
    - Các hàm ảnh hưởng/dính chặt đến nhau -> High Coupling
    - Order bị mở rộng không cần thiết
  - Refactor: Tách thành các interface nhỏ:
    - OrderCommandService (Những method gây side-effect hoặc làm thay đổi dữ liệu db: create/ update/ delete)
    - OrderQueryService (Những method đọc dữ liệu chứ không làm thay đổi: get)
    - ExportService
    - NotificationService

----------------------------------------------------------------------------------------------------------------------------------------------------------

#### *Design Pattern*:

- Strategy Pattern: Discount
  - Problem:
    - Logic discount sử dụng nhiều if-else theo discountCode và customerType
    - Không thể tái sử dụng ở nơi khác
    ```
      if (!StringUtil.isBlank(code)) {...}
      if (!StringUtil.isBlank(request.customerType)) {...Ư
    ```
  - Impact:
    - Khó mở rộng -> phải sửa lại code khi có thêm discountCode hoặc customerType
    - Unit test không thể test riêng cho từng trường hợp
  - Refactor:
    - Mỗi discount là 1 class riêng
    - Mỗi customerType chuyển sang dùng enum

---
- Factory Pattern: chuyển đổi dữ liệu từ request sang entity
  - Problem: map dữ liệu đang thực hiện ngay trong OrderService
    - ```
      OrderItemEntity item = new OrderItemEntity();
      mapToResponse(saved);
      private OrderResponse mapToResponse(OrderEntity order) { ... }
      ```
  - Impact:
    - Business logic và chuyển đổi dữ liệu bị high coupling
    - Không thể tái sử dụng
  - Refactor:
    - Tạo các factory tương ứng để chuyển đổi dữ liệu từ request sang entity (có liên quan đến xử lý logic: setStatus, setLocalDateTime, ...)
    - Tạo các mapper để map dữ liệu từ entity sang response

---

- Observer pattern: Email/ ExportOrderAsCsv
  - Problem: business logic đang high coupling với những method side-effect
    - ```
      public void sendEmail(String to, String content) { ... }
      ```
    - ```
      public String exportOrderAsCsv(Long id) { ... }
      ```
  - Impact:
    - Nếu những side-effect xảy ra lỗi trong quá trình thực hiện các method ở business logic có thể fail toàn hệ thống
    - Không thể mở rộng hệ thống (sendSMS, ...)
  - Refactor: Sử dụng event (Kafka)
    - create order chỉ thực hiện đúng create order
    - sau đó sẽ emit event đến notification-service
    - notification-service: nơi xử lý thông báo email
    - nếu email fail có thể retry và không ảnh hưởng đến create order
    - có thể mở rộng ra (ghi log, queue, sms ...)

----------------------------------------------------------------------------------------------------------------------------------------------------------

#### *Critical Production Issues*:
- Side-effect coupling:
  - Problem: sendEmail() nằm trong createOrder() -> side-effect coupling
  - Impact: create order thành công nhưng email fail -> order được save trong db nhưng vẫn báo lỗi
  - Refactor: Kafka
    - OrderService sau khi create Order sẽ emit event đến notificationService
    - NotificationService là nơi xử lý những event (sendEmail, sendSMS, ghi log, ...)
    - Nếu email fail -> dễ dàng retry ngay tại email -> không ảnh hưởng đến order

---

- Transaction boundary:
  - Problem: Không xác định rõ ranh giới (boundary)
  - Impact:
    - Nếu email fail thì order sẽ rollback hay giữ ?
    - Nếu Order successfully created mà email fail thì vẫn giữ order, không rollback
    - Nếu email fail mà rollback toàn bộ hệ thống sẽ mất luôn order, điều này không cần thiết
  - Refactor:
    - OrderService chỉ thực hiện tạo order
    - Email sẽ được tách ra thành NotificationService để xử lý
    - Notification chạy async và được tách khỏi transaction
    - Nếu email fail thì log lỗi, và retry

---

- Testability:
  - Problem: createOrder chứa nhiều logic + dependency
  - Impact:
    - Không thể test riêng từng trường hợp
    - Phải mock nhiều dependency: customerRepo, productRepo, ...
  - Refactor:
    - Tách riêng ra thành từng service: DiscountService, Validate, NotificationService
    - Sau khi tách có thể test riêng từng logic

---

- Magic number:
  - Problem: ```discount += 50_000;```, ```discount = 25_000;```
  - Impact: fix cứng số, không tái sử dụng, gây rối, khó thay đổi
  - Refactor: dùng constant hoặc enum

---

- DTO Design:
  - Problem: dễ bị thay đổi, không immutable, không đảm bảo tính toàn vẹn dữ liệu
  - Refactor: sử dụng private, getter + setter

----------------------------------------------------------------------------------------------------------------------------------------------------------

#### *KISS*:
- Problem: createOrder quá dài, chứa nhiều logic khác nhau
- Impact: khó đọc, khó debug, khó test
- Refactor: tách thành từng logic nhỏ và OrderService sẽ gọi đến để thực hiện từng bước
  ```
        public OrderResponse createOrder(...) {
          validate(request);
          Customer customer = loadCustomer(request);
          Order order = orderFactory.create(customer);
          List<Item> items = orderItemService.buildItems(request);
          int total = pricingService.calculateTotal(items);
          int discount = discountService.apply(order);
          orderRepository.save(order);
          notificationService.notify(order);
          return orderMapper.toResponse(order);
        }
  ```
  
-------------------------------------------------------------------------------

#### *CLEANCODE and DRY*:
- Problem:
  - Xử lý validate ngay trong create order
  - Validate dữ liệu truyền vào bị lặp
  - Exception không báo lỗi rõ ràng
  - Hard code
    ```
    "NEW"
    "VIP"
    "PROMO10"
    "FREESHIP"
    ...
    ```
    ```
    if (request == null) {...}
    ```
    ```
    else {
        // bad: silently ignore unknown code
        log.warn("Unknown discount code: {}", code);
    }
    ```
  - Impact:
    - Code create phình to ra
    - Không tái sử dụng
    - Lỗi không cụ thể -> không nắm được nguyên nhân
  - Refactor:
    - Sử dụng enum/constants cho các giá trị cố định
    - Tách validate ra class riêng: validateRequest(), validateProduct(), ...
    - Tạo AppException và ErrorCode enum để bắt lỗi rõ ràng và dùng GlobalException để map về cấu trúc JSON -> trả về HTTP status + code + message
    ```
    public class AppException extends RuntimeException {...}
    public enum ErrorCode { ... }


    @ControllerAdvice
    public class GlobalHandleException { ... }
    ```