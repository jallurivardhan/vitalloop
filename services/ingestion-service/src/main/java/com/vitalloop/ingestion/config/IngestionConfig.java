package com.vitalloop.ingestion.config;

import com.vitalloop.ingestion.application.IngestionService;
import com.vitalloop.ingestion.port.EventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free application layer into the Spring context.
 *
 * <p>Keeping the {@link IngestionService} bean definition here (rather than annotating the service)
 * preserves the hexagonal boundary: the application and domain layers stay free of Spring.
 */
@Configuration
public class IngestionConfig {

  @Bean
  public IngestionService ingestionService(EventPublisher eventPublisher) {
    return new IngestionService(eventPublisher);
  }
}
