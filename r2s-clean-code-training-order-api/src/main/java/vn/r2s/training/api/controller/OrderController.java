package vn.r2s.training.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.r2s.training.api.dto.request.CreateOrderRequest;
import vn.r2s.training.api.dto.response.ApiResponse;
import vn.r2s.training.api.dto.response.OrderResponse;
import vn.r2s.training.api.service.OrderCommandService;
import vn.r2s.training.api.service.OrderQueryService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderCommandService commandService;
  private final OrderQueryService queryService;

  @PostMapping
  public ApiResponse<OrderResponse> createOrder(@RequestBody @Valid CreateOrderRequest request) {
    return ApiResponse.<OrderResponse>builder()
        .result(commandService.createOrder(request))
        .build();
  }

  @GetMapping("/{id}")
  public ApiResponse<OrderResponse> getOrderById(@PathVariable("id") Long id) {
    return ApiResponse.<OrderResponse>builder()
        .result(queryService.getOrder(id))
        .build();
  }
}