# Review R3 - ORDER SERVICE + NOTIFICATION SERVICE

## P0 - Rủi ro bảo mật (security + correctness)

- Xoá secrets khỏi repo:
- Đưa các secrets vào ```.env``` và tạo ra ```.env.example``` để chứa các giá trị tương đương
- Cập nhật SecurityConfig: chỉ ```permitAll()``` cho swagger khi ở môi trường ```dev```
- Fix verify order flow: tạo bảng riêng để lưu các thông tin về verify -> thêm version mới vào trong ```db.migration```
- Không log dữ liệu nhạy cảm: chỉ log các thông tin về ```orderId``` và ```customerId``` -> không log ```verifyCode```
- Chuẩn hoá error response: ErrorResponse và ApiResponse, phân loại các exception