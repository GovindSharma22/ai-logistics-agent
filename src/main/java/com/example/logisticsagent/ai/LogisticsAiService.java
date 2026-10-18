package com.example.logisticsagent.ai;

import com.example.logisticsagent.ai.dto.DeliveryRecommendationResponse;
import com.example.logisticsagent.ai.dto.LogisticsInsightsResponse;
import com.example.logisticsagent.order.dto.DeliveryMetricsResponse;
import com.example.logisticsagent.order.dto.DeliveryRiskResponse;
import com.example.logisticsagent.order.service.DeliveryOrderService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
@Service
public class LogisticsAiService {
  static final String NO_AT_RISK_ORDERS_MESSAGE =
    "No at-risk orders were found.";
  private static final String FALLBACK_ACTION =
    "Review this at-risk delivery with operations and act according to its recorded risk level.";
  private static final String FALLBACK_EXPLANATION =
    "This recommendation uses only the application's at-risk order data. No additional operational facts were inferred.";

  private static final Logger log =
    LoggerFactory.getLogger(LogisticsAiService.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


  private final ChatClient chatClient;
  private final DeliveryOrderService deliveryOrderService;

  @Autowired
  public LogisticsAiService(
    ChatClient.Builder chatClientBuilder,
    DeliveryOrderService deliveryOrderService) {
    this.chatClient = chatClientBuilder.build();
    this.deliveryOrderService = deliveryOrderService;
  }


  public LogisticsInsightsResponse getLogisticsInsights(String question) {
    DeliveryMetricsResponse metrics =
      deliveryOrderService.getDeliveryMetrics();
    List<DeliveryRiskResponse> risks =
      deliveryOrderService.getDeliveryRisks();

    String metricsContext = """
                Current delivery metrics:
                - Total orders: %d
                - Delivered orders: %d
                - Delayed orders: %d
                - On-time delivery rate: %s%%

                """.formatted(
      metrics.totalOrders(),
      metrics.deliveredOrders(),
      metrics.delayedOrders(),
      metrics.onTimeDeliveryRate()
    );
    String riskContext = buildRiskContext(risks);
    log.info("AI metrics context:\n{}", metricsContext);
    log.info("AI at-risk order context:\n{}", riskContext);

    if (risks.isEmpty()) {
      String insights = completePrompt(
        systemPrompt(),
        noRiskUserPrompt(metricsContext, question)
      );
      return new LogisticsInsightsResponse(
        metrics,
        withNoAtRiskMessage(insights),
        List.of()
      );
    }

    String modelOutput = completePrompt(
      systemPrompt(),
      recommendationsUserPrompt(metricsContext, riskContext, question)
    );
    AiInsightsPayload payload = parsePayload(modelOutput);

    String insights = buildDeterministicInsights(metrics, risks);

    return new LogisticsInsightsResponse(
      metrics,
      insights,
      mergeRecommendations(risks, payload)
    );
  }

  private String completePrompt(String systemPrompt, String userPrompt) {
    return chatClient.prompt()
      .system(systemPrompt)
      .user(userPrompt)
      .call()
      .content();
  }

  private String buildDeterministicInsights(
    DeliveryMetricsResponse metrics,
    List<DeliveryRiskResponse> risks) {

    String summary = """
            Delivery performance summary:
            - Total orders: %d
            - Delivered orders: %d
            - Delayed orders: %d
            - On-time delivery rate: %s%%

            The on-time delivery rate is calculated using delivered
            orders only. Therefore, a 100%% on-time rate can coexist
            with overdue orders that have not yet been delivered.
            """.formatted(
      metrics.totalOrders(),
      metrics.deliveredOrders(),
      metrics.delayedOrders(),
      metrics.onTimeDeliveryRate()
    );

    if (risks.isEmpty()) {
      return summary + "\nNo at-risk orders were found.";
    }

    return summary + "\nAt-risk orders detected: " + risks.size()
      + ". Review the recommendations for appropriate next actions.";
  }

  private String systemPrompt() {
    return """
        You are an experienced logistics and last-mile delivery
        optimization assistant.

        You receive actual delivery metrics and at-risk order records
        from the application, along with a question from the user.

        Rules:
      - onTimeDeliveryRate is calculated using delivered orders only.
      - totalOrders is not the denominator for onTimeDeliveryRate.
      - An overdue undelivered order does not reduce the on-time
        delivery rate because it has not yet been delivered.
      - The supplied metrics are valid together. Never call them
        inconsistent solely because an undelivered order is overdue.
      - For the supplied example, explain that 1 delivered order
        was on time (100%), while 1 separate order is overdue.
        - Read all supplied metrics and at-risk orders before answering.
        - Use only supplied application data as observed facts.
        - Never invent orders, ETAs, metrics, percentages, or operational events.
        - Clearly distinguish observed facts from recommendations.
        - Do not claim that you executed any operational action.
        - Do not recommend changing an ETA merely to hide a delay.
        - Prioritize actions that can actually reduce delivery delays.
        - Consider these possible actions when relevant:
          1. Verify warehouse picking and dispatch readiness.
          2. Check delivery-partner availability and route assignment.
          3. Review route congestion or other operational constraints,
             but only when supporting data is available.
          4. Recalculate the ETA using reliable, current information.
          5. Notify the customer if a delay is confirmed.
          6. Escalate unresolved high-risk deliveries to operations.
                  - Interpret onTimeDeliveryRate as the percentage of delivered
                  orders that were delivered on or before their estimated ETA.
                - A 100% on-time delivery rate can be correct even when
                  undelivered orders are overdue.
                - delayedOrders and overdueUndeliveredOrders represent
                  different operational metrics and may overlap.
                - Do not describe metrics as inconsistent unless the supplied
                  values contradict their definitions.
                - When explaining onTimeDeliveryRate, clarify that its
                  denominator is delivered orders only.
        - Do not assume that picking is incomplete, a route is congested,
          or a delivery partner is unavailable unless the data confirms it.
        - If additional data is needed, state what should be checked.
        - If the at-risk order list is empty, include this exact sentence:
          "No at-risk orders were found."
        """;
  }


  private String noRiskUserPrompt(String metricsContext, String question) {
    return """
      DELIVERY METRICS FROM THE APPLICATION:
      %s

      AT-RISK ORDERS FROM THE APPLICATION:
      None.

      USER QUESTION:
      %s

      Analyze the supplied metrics directly. If the dataset is
      small, explain that limitation without ignoring the data.
      Clearly state that no at-risk orders were found. Do not invent
      orders, metrics, ETAs, or operational facts.
      """.formatted(metricsContext, question);
  }


  private String recommendationsUserPrompt(
    String metricsContext,
    String riskContext,
    String question
  ) {
    return """
        DELIVERY METRICS FROM THE APPLICATION:
        %s

        AT-RISK ORDERS FROM THE APPLICATION:
        %s

        USER QUESTION:
        %s

        Analyze the supplied metrics and at-risk orders directly.
        Explain any limitations caused by the small dataset.

        For each at-risk order:
        - Choose one practical next operational action.
        - Base the action on the supplied status, risk level, and reason.
        - For a high-risk order whose ETA has passed, prioritize checking
          its current location, dispatch status, and delivery progress.
        - Recommend recalculating the ETA only after verifying current
          delivery conditions.
        - Do not invent a root cause or claim an action has been completed.
        - Explain why the action is appropriate and identify missing data
          when the root cause cannot be established.

        Return JSON only, with this exact structure:
        {
          "insights": "natural language analysis of the supplied data",
          "recommendations": [
            {
              "orderNumber": "exact supplied order number",
              "recommendedAction": "specific next operational action",
              "explanation": "evidence-based reason for the action"
            }
          ]
        }

        Include exactly one recommendation for every supplied at-risk order.
        Do not include orders that were not supplied.
        Do not invent metrics, ETAs, or operational facts.
        """.formatted(metricsContext, riskContext, question);
  }



  private String buildRiskContext(List<DeliveryRiskResponse> risks) {
    if (risks.isEmpty()) {
      return "None.";
    }

    LocalDateTime now = LocalDateTime.now();

    return risks.stream()
      .map(risk -> {
        String eta = risk.estimatedDeliveryAt() == null
          ? "unknown"
          : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
          risk.estimatedDeliveryAt()
        );

        String timing;
        if (risk.estimatedDeliveryAt() == null) {
          timing = "ETA is unavailable; timing cannot be determined.";
        } else if (risk.estimatedDeliveryAt().isBefore(now)) {
          long overdueMinutes = java.time.Duration.between(
            risk.estimatedDeliveryAt(), now
          ).toMinutes();

          timing = "OVERDUE by approximately "
            + overdueMinutes + " minutes.";
        } else {
          long minutesRemaining = java.time.Duration.between(
            now, risk.estimatedDeliveryAt()
          ).toMinutes();

          timing = "ETA is approximately "
            + minutesRemaining + " minutes from now.";
        }

        return """
                - Order number: %s
                  Customer: %s
                  Status: %s
                  ETA: %s
                  Risk level: %s
                  Risk reason: %s
                  Timing assessment: %s
                """.formatted(
          risk.orderNumber(),
          risk.customerName(),
          risk.status(),
          eta,
          risk.riskLevel(),
          risk.reason(),
          timing
        );
      })
      .collect(Collectors.joining("\n"));
  }


  private String withNoAtRiskMessage(String insights) {
    if (isBlank(insights)) {
      return NO_AT_RISK_ORDERS_MESSAGE;
    }
    String trimmed = insights.trim();
    if (trimmed.contains(NO_AT_RISK_ORDERS_MESSAGE)) {
      return trimmed;
    }
    return NO_AT_RISK_ORDERS_MESSAGE + "\n\n" + trimmed;
  }

  private String defaultInsights(String modelOutput) {
    String cleaned = stripModelNoise(modelOutput);
    return isBlank(cleaned) ? "Unable to parse AI insights." : cleaned;
  }

  private List<DeliveryRecommendationResponse> mergeRecommendations(
    List<DeliveryRiskResponse> risks,
    AiInsightsPayload payload
  ) {
    Map<String, AiRecommendation> byOrderNumber = new LinkedHashMap<>();
    if (payload != null && payload.recommendations() != null) {
      for (AiRecommendation recommendation : payload.recommendations()) {
        if (recommendation == null || isBlank(recommendation.orderNumber())) {
          continue;
        }
        byOrderNumber.putIfAbsent(
          recommendation.orderNumber().trim(),
          recommendation
        );
      }
    }

    List<DeliveryRecommendationResponse> recommendations = new ArrayList<>();
    for (DeliveryRiskResponse risk : risks) {
      AiRecommendation generated = byOrderNumber.get(risk.orderNumber());
      String action = generated != null && !isBlank(generated.recommendedAction())
        ? generated.recommendedAction().trim()
        : FALLBACK_ACTION;
      String explanation = generated != null && !isBlank(generated.explanation())
        ? generated.explanation().trim()
        : FALLBACK_EXPLANATION;
      recommendations.add(new DeliveryRecommendationResponse(
        risk.orderNumber(),
        risk.riskLevel(),
        risk.reason(),
        action,
        explanation
      ));
    }
    return List.copyOf(recommendations);
  }

  private AiInsightsPayload parsePayload(String modelOutput) {
    String json = extractJson(modelOutput);
    if (json == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(json, AiInsightsPayload.class);
    } catch (Exception ex) {
      log.warn("Unable to parse AI recommendation JSON", ex);
      return null;
    }
  }

  private String extractJson(String modelOutput) {
    String cleaned = stripModelNoise(modelOutput);
    if (isBlank(cleaned)) {
      return null;
    }
    int start = cleaned.indexOf('{');
    int end = cleaned.lastIndexOf('}');
    if (start < 0 || end <= start) {
      return null;
    }
    return cleaned.substring(start, end + 1);
  }

  private String stripModelNoise(String modelOutput) {
    if (modelOutput == null) {
      return "";
    }
    String cleaned = modelOutput
      .replaceAll("(?s)<think>.*?</think>", "")
      .trim();
    if (cleaned.startsWith("```")) {
      cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
      cleaned = cleaned.replaceFirst("\\s*```$", "");
    }
    return cleaned.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record AiInsightsPayload(
    String insights,
    List<AiRecommendation> recommendations
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record AiRecommendation(
    String orderNumber,
    String recommendedAction,
    String explanation
  ) {}
}
