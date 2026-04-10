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

## Clone repo và chạy thành công: `docker compose up -d`

## Phân tích source code:

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

## Input/ Output:

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
     "discountCents": 25000,
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

## Những vấn đề về source code

### 1. SRP – Single Responsibility Principle

### 1.1 Definition:

- Mỗi class chỉ thực hiện 1 việc duy nhất

### 1.2 Mapping to code:

```
public OrderResponse createOrder(CreateOrderRequest request) {
    if (request == null) throw new BadRequestException("request is null");
    if (StringUtil.isBlank(request.customerEmail)) throw new BadRequestException("email is blank");
    if (request.items == null || request.items.isEmpty()) throw new BadRequestException("items empty");

    CustomerEntity customer = customerRepo.findByEmail(request.customerEmail)
        .orElseThrow(() -> new NotFoundException("Customer not found: " + request.customerEmail));

    OrderEntity order = new OrderEntity();
    order.setCustomer(customer);
    order.setStatus("NEW"); // magic string
    order.setCreatedAt(LocalDateTime.now());

    // Items building + calculation (SRP violation)
    ...
    }
```

- createOrder() đang xử lý quá nhiều việc
    - validate request
    - load customer
    - build entity
    - calculate total
    - apply discount
    - save DB
    - send email
    - mapping response

### 1.3 Problem:

- Method quá dài -> God method
- Business logic và side effect bị trộn lẫn

### 1.4 Impact:

- Phải mock nhiều dependency khi test -> không test logic cho từng chức năng được
- Không tái sử dụng code

## 2. OCP – Open Closed Principle

### 2.1 Definition:

- Mở rộng nhưng không được sửa code
- Khi thêm chức năng -> viết class mới chứ không sửa code cũ

### 2.2 Mapping to code:

```
...
    if (!StringUtil.isBlank(code)) {
      if ("PROMO10".equalsIgnoreCase(code)) {
        discount = (int) (total * 0.10);
      } else if ("PROMO20".equalsIgnoreCase(code) && total >= 10_000_000) {
        discount = (int) (total * 0.20);
      } else if ("FREESHIP".equalsIgnoreCase(code)) {
        discount = 25_000; // nonsense in cents, intentional smell
      } else {
        // bad: silently ignore unknown code
        log.warn("Unknown discount code: {}", code);
      }
    }

    // customerType influences discount - again long if-else
    if (!StringUtil.isBlank(request.customerType)) {
      if ("vip".equalsIgnoreCase(request.customerType)) {
        discount += 50_000;
      } else if ("new".equalsIgnoreCase(request.customerType)) {
        discount += 10_000;
      }
    }
...
```

### 2.3 Problem:

- Hard code
- Có thêm discount phải sửa code

### 2.4 Impact:

- Không mở rộng
- Dễ bug khi thay đổi code

## 3. DIP – Dependency Inversion Principle

### 3.1 Definition:

- High-level module (service) không nên phụ thuộc trực tiếp vào Low-level module (repository) mà cả
  2 nên phụ thuộc vào abstraction (interface/ abstract class)
- Dùng interface và inject vào để sử dụng

### 3.2 Mapping to code:

```
...
if ("vip".equalsIgnoreCase(request.customerType)) {
    sendEmail(customer.getEmail(), "Thanks VIP! Your order id = " + saved.getId());
}
...

@Override
public void sendEmail(String to, String content) {
    System.out.println("SENDING EMAIL TO " + to + " with content = " + content);
}
...
```

### 3.3 Problem:

- OrderService tự thực hiện luôn việc gửi mail

### 3.4 Impact

- High coupling (kết nối chặt chẽ): nếu thay đổi code phải sửa nhiều chỗ
- Không thể mở rộng (email -> SMS)

## 4. ISP – Interface Segregation Principle

### 4.1 Definition:

- Không ép class implement những method mà nó không cần
- Nên tạo các interface đúng mục đích, không tạo interface chứa nhiều thứ

### 4.2 Mapping to code:

```
public interface OrderService {
  OrderResponse createOrder(CreateOrderRequest request);
  OrderResponse getOrder(Long id);
  void sendEmail(String to, String content);
  String exportOrderAsCsv(Long id);
}
```

### 4.3 Problem:

- Interface quá lớn
- Có class chỉ read mà phải thực hiện hết

### 4.4 Impact:

- High coupling

## 5. DESIGN PATTERN ISSUES: Strategy Pattern

### 5.1 Definition:

- Tách các hành vi xử lý thành các class riêng

### 5.2 Mapping to code:

```
...
if (!StringUtil.isBlank(code)) {
      if ("PROMO10".equalsIgnoreCase(code)) {
        discount = (int) (total * 0.10);
      } else if ("PROMO20".equalsIgnoreCase(code) && total >= 10_000_000) {
        discount = (int) (total * 0.20);
      } else if ("FREESHIP".equalsIgnoreCase(code)) {
        discount = 25_000; // nonsense in cents, intentional smell
      } else {
        // bad: silently ignore unknown code
        log.warn("Unknown discount code: {}", code);
      }
    }

    // customerType influences discount - again long if-else
    if (!StringUtil.isBlank(request.customerType)) {
      if ("vip".equalsIgnoreCase(request.customerType)) {
        discount += 50_000;
      } else if ("new".equalsIgnoreCase(request.customerType)) {
        discount += 10_000;
      }
    }
...
```
### 5.3 Problem:
- Vi phạm OCP
- Không thể tái sử dụng

### 5.4 Impact:
- Khi có thêm hành vi mới phải sửa code (viết thêm if-else)

[//]: # (## 6. DESIGN PATTERN: Factory Pattern)

[//]: # (### 6.1 Definition:)

[//]: # (- Tạo object mới thông qua factory, ẩn logic tạo object)

[//]: # ()
[//]: # (### 6.2 Mapping to code:)

[//]: # (```)

[//]: # (new OrderEntity&#40;&#41;)

[//]: # (new OrderItemEntity&#40;&#41;)

[//]: # (```)

[//]: # (### 6.3 Problem:)

[//]: # (- Logic tạo object nằm trong business logic của createOrder&#40;&#41;)

## 6. DESIGN PATTERN: Observer Pattern
### 6.1 Definition:
- Khi một đối tượng thay đổi, các đối tượng khác (observer) sẽ được thông báo tự động.
- 1 nơi phát sự kiện (Publisher / Subject)
- Nhiều nơi nghe (Observer / Subscriber)
- Khi có thay đổi → tất cả observer được notify

### 6.2 Mapping to code:
```
...
sendEmail(...)
...
```
### 6.3 Problem:
- Email (side-effect) nằm trong business logic
### 6.4 Impact:
- Email fail -> createOrder cũng fail

## 7. DRY
### 7.1 Definition:
- Không viết lại cùng một logic nhiều lần ở nhiều chỗ.
### 7.2 Mapping to code:
```
...
if (request == null) throw new BadRequestException("request is null");
if (StringUtil.isBlank(request.customerEmail)) throw new BadRequestException("email is blank");
if (request.items == null || request.items.isEmpty()) throw new BadRequestException("items empty");
...
```
### 7.3 Problem:
- Duplicate code
### 7.4 Impact:
- Nơi khác sử dụng logic code tương tự -> copy paste sang chỗ cần

## 8. KISS
### 8.1 Definition:
- Code đơn giản nhất có thể, đừng làm phức tạp khi chưa cần, không quá dài
### 8.2 Mapping to code:
```
if ("PROMO10".equalsIgnoreCase(code)) {
...
} else if ("PROMO20".equalsIgnoreCase(code) ...
```
### 8.3 Problem
- Nếu phát triển mở rộng thêm thì if-else quá dài, quá phức tạp

## 9. Clean code
- Code dễ đọc, dễ hiểu, dễ sửa
- Tên rõ nghĩa (Meaningful names)
- Hàm ngắn, làm 1 việc (SRP)
- Không lặp code (DRY)
- Ít if-else phức tạp
- Không hard code

