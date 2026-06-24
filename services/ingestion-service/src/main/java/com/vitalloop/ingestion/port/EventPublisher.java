package com.vitalloop.ingestion.port;

import com.vitalloop.contracts.VitalsReadingReceived;

/**
 * Outbound port for publishing domain events.
 *
 * <p>The application depends only on this interface; the Pub/Sub implementation lives in the
 * messaging adapter, keeping the application and domain layers transport-agnostic.
 */
public interface EventPublisher {

  /**
   * Publishes a {@link VitalsReadingReceived} event.
   *
   * @param event the event to publish (never {@code null})
   */
  void publish(VitalsReadingReceived event);
}
