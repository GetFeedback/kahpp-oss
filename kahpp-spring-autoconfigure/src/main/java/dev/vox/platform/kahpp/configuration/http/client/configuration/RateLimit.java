package dev.vox.platform.kahpp.configuration.http.client.configuration;

import java.time.Duration;
import org.apache.http.HttpRequest;

public interface RateLimit {
  RateLimit NO_LIMIT =
      new RateLimit() {
        @Override
        public boolean acquirePermit(HttpRequest httpRequest) {
          return true;
        }

        @Override
        public Duration waitingTimeout() {
          return Duration.ZERO;
        }
      };

  boolean acquirePermit(HttpRequest httpRequest);

  Duration waitingTimeout();
}
