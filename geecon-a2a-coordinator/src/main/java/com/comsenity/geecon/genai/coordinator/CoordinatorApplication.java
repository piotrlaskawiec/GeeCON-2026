package com.comsenity.geecon.genai.coordinator;

import com.comsenity.geecon.genai.coordinator.guardrail.GuardrailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GuardrailProperties.class)
public class CoordinatorApplication {

    static void main(String[] args) {
        SpringApplication.run(CoordinatorApplication.class, args);
    }
}
