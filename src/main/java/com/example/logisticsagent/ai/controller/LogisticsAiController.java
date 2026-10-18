
package com.example.logisticsagent.ai.controller;

import com.example.logisticsagent.ai.LogisticsAiService;
import com.example.logisticsagent.ai.dto.LogisticsInsightsResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class LogisticsAiController {

  private final LogisticsAiService logisticsAiService;

  public LogisticsAiController(LogisticsAiService logisticsAiService) {
    this.logisticsAiService = logisticsAiService;
  }

  @PostMapping("/logistics-insights")
  @ResponseStatus(HttpStatus.OK)
  public LogisticsInsightsResponse getLogisticsInsights(
    @Valid @RequestBody LogisticsInsightsRequest request) {
    return logisticsAiService.getLogisticsInsights(request.question());
  }

  public record LogisticsInsightsRequest(
    @NotBlank(message = "Question must not be blank")
    @Size(max = 2000, message = "Question must not exceed 2000 characters")
    String question
  ) {}
}
