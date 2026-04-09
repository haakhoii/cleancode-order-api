```
khong co async

createOrder()
↓
publishEvent
↓
listener.handle()
↓
call notification (sleep 10s)
↓
👉 API trả về (sau 10s)
```

```
co async

createOrder()
   ↓
publishEvent
   ↓
Spring sẽ chạy cái này ở thread khác
   ↓
👉 API RETURN NGAY ✅

(sau đó mới chạy tiếp)
   ↓
listener.handle() (background)
   ↓
call notification (sleep 10s)
```

```
Thread 1 (API)
----------------
createOrder
publishEvent
👉 RETURN RESPONSE NGAY ✅


Thread 2 (Async)
----------------
handle()
Feign call
⏳ chờ 5s
💥 timeout
log error
```