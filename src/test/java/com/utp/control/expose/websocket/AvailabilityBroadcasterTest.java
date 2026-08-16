package com.utp.control.expose.websocket;

import com.utp.control.generated.model.LocationAvailability;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class AvailabilityBroadcasterTest {

  private LocationAvailability availability(int available) {
    return new LocationAvailability()
        .locationId(1)
        .locationName("Sede Arequipa")
        .capacity(75)
        .occupied(75 - available)
        .available(available);
  }

  @Test
  void testAvailabilityChanges_DeliversPublishedEventsToConnectedClients() {
    AvailabilityBroadcaster broadcaster = new AvailabilityBroadcaster();

    StepVerifier.create(broadcaster.availabilityChanges())
        .then(() -> broadcaster.publishAvailabilityChanged(availability(32)).subscribe())
        .assertNext(event -> {
          assert event.getLocationId() == 1;
          assert event.getAvailable() == 32;
        })
        .then(() -> broadcaster.publishAvailabilityChanged(availability(33)).subscribe())
        .assertNext(event -> {
          assert event.getAvailable() == 33;
        })
        .thenCancel()
        .verify();
  }

  @Test
  void testPublishAvailabilityChanged_WithoutSubscribersDoesNotFail() {
    AvailabilityBroadcaster broadcaster = new AvailabilityBroadcaster();

    StepVerifier.create(broadcaster.publishAvailabilityChanged(availability(32)))
        .verifyComplete();
  }
}
