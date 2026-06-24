package com.vitalloop.triage.adapter.messaging;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the live Pub/Sub subscription once the application is ready.
 *
 * <p>Restricted to the {@code local} profile so the build, CI, and unit/slice tests never attempt
 * to connect to a Pub/Sub backend. The end-to-end flow runs under {@code make dev-up}.
 */
@Component
@Profile("local")
public class PubSubSubscriberInitializer {

  private static final Logger log = LoggerFactory.getLogger(PubSubSubscriberInitializer.class);

  private final PubSubTemplate pubSubTemplate;
  private final VitalsEventConsumer consumer;
  private final String subscription;

  public PubSubSubscriberInitializer(
      PubSubTemplate pubSubTemplate,
      VitalsEventConsumer consumer,
      @Value("${vitalloop.pubsub.subscription:vitals-reading-received-sub}") String subscription) {
    this.pubSubTemplate = pubSubTemplate;
    this.consumer = consumer;
    this.subscription = subscription;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void subscribe() {
    log.atInfo()
        .setMessage("subscribing to Pub/Sub subscription")
        .addKeyValue("subscription", subscription)
        .log();
    pubSubTemplate.subscribe(subscription, consumer::handleMessage);
  }
}
