
package com.example.logisticsagent.order.repository;

import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
import java.util.ArrayList;
@DataJpaTest
class DeliveryOrderRepositoryTest {

  @Autowired
  private DeliveryOrderRepository repository;

  @Test
  void shouldSearchByCustomerNameAndStatus() {
    DeliveryOrder order = new DeliveryOrder(
      "ORD-TEST-001",
      "Rahul Sharma",
      "Jaipur, Rajasthan",
      LocalDateTime.now().plusHours(2)
    );

    order.markDelivered(LocalDateTime.now());
    repository.saveAndFlush(order);

    Page<DeliveryOrder> result = repository.searchOrders(
      "Rahul",
      DeliveryStatus.DELIVERED,
      PageRequest.of(0, 10)
    );

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getOrderNumber())
      .isEqualTo("ORD-TEST-001");
  }

  @Test
  void shouldReturnNoResultsWhenStatusDoesNotMatch() {
    DeliveryOrder order = new DeliveryOrder(
      "ORD-TEST-002",
      "Rahul Sharma",
      "Jaipur, Rajasthan",
      LocalDateTime.now().plusHours(2)
    );

    repository.saveAndFlush(order);

    Page<DeliveryOrder> result = repository.searchOrders(
      "Rahul",
      DeliveryStatus.DELAYED,
      PageRequest.of(0, 10)
    );

    assertThat(result.getContent()).isEmpty();
  }
}
