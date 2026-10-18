package com.example.logisticsagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@SpringBootApplication
public class AiLogisticsAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiLogisticsAgentApplication.class, args);
	}

}
