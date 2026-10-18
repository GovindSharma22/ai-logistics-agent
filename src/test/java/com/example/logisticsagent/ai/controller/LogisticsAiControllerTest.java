
package com.example.logisticsagent.ai.controller;

import com.example.logisticsagent.ai.LogisticsAiService;
import com.example.logisticsagent.order.dto.DeliveryMetricsResponse;
import com.example.logisticsagent.order.service.DeliveryOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.example.logisticsagent.ai.dto.DeliveryRecommendationResponse;
import com.example.logisticsagent.ai.dto.LogisticsInsightsResponse;
import java.util.List;
@WebMvcTest(LogisticsAiController.class)
class LogisticsAiControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private LogisticsAiService logisticsAiService;

  @MockitoBean
  private DeliveryOrderService deliveryOrderService;

  @Test
  void shouldReturnMetricsAndAiInsights() throws Exception {
    DeliveryMetricsResponse metrics = new DeliveryMetricsResponse(
      3L,
      1L,
      1L,
      new BigDecimal("100.00"),
      1L
    );

    LogisticsInsightsResponse response = new LogisticsInsightsResponse(
      metrics,
      "One order delivered on time.",
      List.<DeliveryRecommendationResponse>of()
    );

    given(logisticsAiService.getLogisticsInsights(
      "Analyze our current delivery performance."
    )).willReturn(response);

    given(deliveryOrderService.getDeliveryMetrics())
      .willReturn(metrics);

    mockMvc.perform(post("/api/ai/logistics-insights")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                                {
                                  "question": "Analyze our current delivery performance."
                                }
                                """))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.metrics.totalOrders").value(3))
      .andExpect(jsonPath("$.metrics.deliveredOrders").value(1))
      .andExpect(jsonPath("$.metrics.delayedOrders").value(1))
      .andExpect(jsonPath("$.metrics.onTimeDeliveryRate").value(100.00))
      .andExpect(jsonPath("$.metrics.overdueUndeliveredOrders").value(1))
      .andExpect(jsonPath("$.insights")
        .value("One order delivered on time."));
  }


  @Test
  void shouldRejectBlankQuestion() throws Exception {
    mockMvc.perform(post("/api/ai/logistics-insights")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                    {"question": "   "}
                    """))
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldRejectQuestionLongerThan2000Characters() throws Exception {
    String longQuestion = "a".repeat(2001);
    String requestBody = """
            {"question": "%s"}
            """.formatted(longQuestion);

    mockMvc.perform(post("/api/ai/logistics-insights")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest());
  }

}
