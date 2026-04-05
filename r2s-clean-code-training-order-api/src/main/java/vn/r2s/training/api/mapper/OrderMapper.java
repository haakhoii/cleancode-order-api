package vn.r2s.training.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.r2s.training.api.dto.response.OrderItemsResponse;
import vn.r2s.training.api.dto.response.OrderResponse;
import vn.r2s.training.api.entity.OrderEntity;
import vn.r2s.training.api.entity.OrderItemEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

  @Mapping(source = "customer.email", target = "customerEmail")
  @Mapping(source = "status", target = "status")
  OrderResponse toResponse(OrderEntity order);

  List<OrderItemsResponse> toItemResponses(List<OrderItemEntity> items);

  @Mapping(source = "product.sku", target = "sku")
  OrderItemsResponse toItemResponse(OrderItemEntity item);
}