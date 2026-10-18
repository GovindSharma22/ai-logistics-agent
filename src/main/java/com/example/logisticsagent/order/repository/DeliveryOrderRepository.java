package com.example.logisticsagent.order.repository;

import java.util.Collection;
import java.util.List;
import com.example.logisticsagent.order.entity.DeliveryOrder;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeliveryOrderRepository
  extends JpaRepository<DeliveryOrder, Long> {

  Optional<DeliveryOrder> findByOrderNumber(String orderNumber);

  boolean existsByOrderNumber(String orderNumber);

  List<DeliveryOrder> findByStatus(DeliveryStatus status);
  Page<DeliveryOrder> findByStatus(
    DeliveryStatus status,
    Pageable pageable
  );
  List<DeliveryOrder> findByEstimatedDeliveryAtBeforeAndStatusNotIn(
    LocalDateTime time,
    Collection<DeliveryStatus> excludedStatuses
  );

//  long countByStatus(DeliveryStatus status);

  long countByStatus(DeliveryStatus status);


  @Query("""
    SELECT COUNT(o)
    FROM DeliveryOrder o
    WHERE o.status = com.example.logisticsagent.order.entity.DeliveryStatus.DELIVERED
      AND o.actualDeliveryAt IS NOT NULL
      AND o.actualDeliveryAt <= o.estimatedDeliveryAt
    """)
  long countDeliveredOnTime();

  Page<DeliveryOrder> findByOrderNumberContainingIgnoreCase(
    String orderNumber,
    Pageable pageable
  );

  Page<DeliveryOrder> findByCustomerNameContainingIgnoreCase(
    String customerName,
    Pageable pageable
  );

  @Query("""
    SELECT o
    FROM DeliveryOrder o
    WHERE (
        LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :search, '%'))
        OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
    )
    AND (:status IS NULL OR o.status = :status)
    """)
  Page<DeliveryOrder> searchOrders(
    @org.springframework.data.repository.query.Param("search")
    String search,
    @org.springframework.data.repository.query.Param("status")
    DeliveryStatus status,
    Pageable pageable
  );

  @Query("""
    SELECT COUNT(o)
    FROM DeliveryOrder o
    WHERE o.estimatedDeliveryAt < :now
      AND o.status NOT IN (
        com.example.logisticsagent.order.entity.DeliveryStatus.DELIVERED,
        com.example.logisticsagent.order.entity.DeliveryStatus.CANCELLED
      )
    """)
  long countOverdueUndelivered(
    @org.springframework.data.repository.query.Param("now")
    LocalDateTime now
  );

}
