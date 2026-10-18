
package com.example.logisticsagent.order.controller;

import com.example.logisticsagent.order.service.DeliveryOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.example.logisticsagent.order.dto.PaginatedOrdersResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.example.logisticsagent.order.entity.DeliveryStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import com.example.logisticsagent.order.exception.GlobalExceptionHandler;
import com.example.logisticsagent.order.exception.InvalidStatusTransitionException;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.when;


@WebMvcTest(DeliveryOrderController.class)
@Import(GlobalExceptionHandler.class)
class DeliveryOrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DeliveryOrderService service;

  @Test
  void shouldRejectNegativePageNumber() throws Exception {
    mockMvc.perform(get("/api/orders/page")
        .param("page", "-1")
        .param("size", "10"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message")
        .value("Page must be zero or greater"));
  }

  @Test
  void shouldRejectPageSizeAbove100() throws Exception {
    mockMvc.perform(get("/api/orders/page")
        .param("page", "0")
        .param("size", "101"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message")
        .value("Size must be between 1 and 100"));
  }

  @Test
  void shouldRejectInvalidSortField() throws Exception {
    mockMvc.perform(get("/api/orders/page")
        .param("page", "0")
        .param("size", "10")
        .param("sortBy", "password"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message")
        .value("Invalid sort field: password"));
  }

  @Test
  void shouldRejectInvalidSortDirection() throws Exception {
    mockMvc.perform(get("/api/orders/page")
        .param("page", "0")
        .param("size", "10")
        .param("direction", "wrong"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message")
        .value(org.hamcrest.Matchers.containsString(
          "either 'desc' or 'asc'")));
  }

  @Test
  void shouldReturnPaginatedOrdersSuccessfully() throws Exception {
    when(service.getAllPaginated(any(Pageable.class)))
      .thenReturn(new org.springframework.data.domain.PageImpl<>(
        java.util.List.of(),
        org.springframework.data.domain.PageRequest.of(0, 10),
        0
      ));

    mockMvc.perform(get("/api/orders/page")
        .param("page", "0")
        .param("size", "10")
        .param("sortBy", "createdAt")
        .param("direction", "desc"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orders").isArray())
      .andExpect(jsonPath("$.orders.length()").value(0))
      .andExpect(jsonPath("$.page").value(0))
      .andExpect(jsonPath("$.size").value(10))
      .andExpect(jsonPath("$.totalElements").value(0))
      .andExpect(jsonPath("$.totalPages").value(0))
      .andExpect(jsonPath("$.first").value(true))
      .andExpect(jsonPath("$.last").value(true));
  }


  @Test
  void shouldFilterPaginatedOrdersByDelayedStatus() throws Exception {
    when(service.getPaginatedByStatus(
      org.mockito.ArgumentMatchers.eq(DeliveryStatus.DELAYED),
      any(Pageable.class)))
      .thenReturn(new PageImpl<>(
        java.util.List.of(),
        PageRequest.of(0, 10),
        0
      ));

    mockMvc.perform(get("/api/orders/page")
        .param("status", "DELAYED")
        .param("page", "0")
        .param("size", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orders").isArray())
      .andExpect(jsonPath("$.totalElements").value(0));

    verify(service).getPaginatedByStatus(
      org.mockito.ArgumentMatchers.eq(DeliveryStatus.DELAYED),
      any(Pageable.class));
  }

  @Test
  void shouldFilterPaginatedOrdersByDeliveredStatus() throws Exception {
    when(service.getPaginatedByStatus(
      org.mockito.ArgumentMatchers.eq(DeliveryStatus.DELIVERED),
      any(Pageable.class)))
      .thenReturn(new PageImpl<>(
        java.util.List.of(),
        PageRequest.of(0, 10),
        0
      ));

    mockMvc.perform(get("/api/orders/page")
        .param("status", "DELIVERED")
        .param("page", "0")
        .param("size", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orders").isArray())
      .andExpect(jsonPath("$.totalElements").value(0));

    verify(service).getPaginatedByStatus(
      org.mockito.ArgumentMatchers.eq(DeliveryStatus.DELIVERED),
      any(Pageable.class));
  }


  @Test
  void shouldSearchByCustomerNameAndStatus() throws Exception {
    when(service.searchOrders(
      eq("Rahul"),
      eq(DeliveryStatus.DELIVERED),
      any(Pageable.class)
    )).thenReturn(new PageImpl<>(
      java.util.List.of(),
      PageRequest.of(0, 10),
      0
    ));

    mockMvc.perform(get("/api/orders/page")
        .param("search", "Rahul")
        .param("status", "DELIVERED")
        .param("page", "0")
        .param("size", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orders").isArray())
      .andExpect(jsonPath("$.totalElements").value(0));

    verify(service).searchOrders(
      eq("Rahul"),
      eq(DeliveryStatus.DELIVERED),
      any(Pageable.class)
    );
  }

  @Test
  void shouldSearchByOrderNumberAndStatus() throws Exception {
    when(service.searchOrders(
      eq("ORD-1001"),
      eq(DeliveryStatus.DELIVERED),
      any(Pageable.class)
    )).thenReturn(new PageImpl<>(
      java.util.List.of(),
      PageRequest.of(0, 10),
      0
    ));

    mockMvc.perform(get("/api/orders/page")
        .param("search", "ORD-1001")
        .param("status", "DELIVERED")
        .param("page", "0")
        .param("size", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.orders").isArray())
      .andExpect(jsonPath("$.totalElements").value(0));

    verify(service).searchOrders(
      eq("ORD-1001"),
      eq(DeliveryStatus.DELIVERED),
      any(Pageable.class)
    );
  }

  @Test
  void shouldReturnConflictForInvalidStatusTransition() throws Exception {
    when(service.updateStatus(eq(1L), eq(DeliveryStatus.OUT_FOR_DELIVERY)))
      .thenThrow(new InvalidStatusTransitionException(
        "Cannot change order status from CREATED to OUT_FOR_DELIVERY"
      ));

    mockMvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
          .patch("/api/orders/1/status")
          .param("status", "OUT_FOR_DELIVERY")
      )
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.status").value(409))
      .andExpect(jsonPath("$.error").value("Conflict"))
      .andExpect(jsonPath("$.message").value(
        "Cannot change order status from CREATED to OUT_FOR_DELIVERY"
      ));
  }


}
