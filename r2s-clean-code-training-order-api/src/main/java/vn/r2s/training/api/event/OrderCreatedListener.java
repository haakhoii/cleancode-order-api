//package vn.r2s.training.api.event;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class OrderCreatedListener {
//
//  private final NotifierService notifierService;
//
//  @EventListener
//  public void handle(OrderCreatedEvent event) {
//
//    if ("VIP".equalsIgnoreCase(event.getCustomerType().name())) {
//      notifierService.send(
//          event.getCustomerEmail(),
//          "Thanks VIP! Order id = " + event.getOrderId()
//      );
//    }
//  }
//}
