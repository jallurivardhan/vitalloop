package com.vitalloop.triage.config;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code vitals-reading-received} topic and {@code vitals-reading-received-sub}
 * subscription on the local Pub/Sub emulator if they do not already exist, so the end-to-end flow
 * works out of the box with {@code make dev-up}.
 *
 * <p>Runs on {@link ApplicationStartedEvent}, which fires before the {@code ApplicationReadyEvent}
 * used by {@code PubSubSubscriberInitializer}; this guarantees the subscription exists before the
 * subscriber attaches to it (otherwise the streaming pull fails permanently with NOT_FOUND).
 *
 * <p>Restricted to the {@code local} profile; it never runs in the default build or CI.
 */
@Component
@Profile("local")
public class PubSubLocalInitializer {

  private static final Logger log = LoggerFactory.getLogger(PubSubLocalInitializer.class);

  private final PubSubAdmin pubSubAdmin;
  private final String topic;
  private final String subscription;

  public PubSubLocalInitializer(
      PubSubAdmin pubSubAdmin,
      @Value("${vitalloop.pubsub.topic:vitals-reading-received}") String topic,
      @Value("${vitalloop.pubsub.subscription:vitals-reading-received-sub}") String subscription) {
    this.pubSubAdmin = pubSubAdmin;
    this.topic = topic;
    this.subscription = subscription;
  }

  @EventListener(ApplicationStartedEvent.class)
  public void ensureTopicAndSubscription() {
    if (pubSubAdmin.getTopic(topic) == null) {
      pubSubAdmin.createTopic(topic);
      log.atInfo().setMessage("created Pub/Sub topic").addKeyValue("topic", topic).log();
    }
    if (pubSubAdmin.getSubscription(subscription) == null) {
      pubSubAdmin.createSubscription(subscription, topic);
      log.atInfo()
          .setMessage("created Pub/Sub subscription")
          .addKeyValue("subscription", subscription)
          .addKeyValue("topic", topic)
          .log();
    }
  }
}
