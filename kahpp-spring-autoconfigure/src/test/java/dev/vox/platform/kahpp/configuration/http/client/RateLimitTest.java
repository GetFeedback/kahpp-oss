package dev.vox.platform.kahpp.configuration.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimit;
import java.time.Duration;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.Test;

class RateLimitTest {
  @Test
  void noLimitShouldHaveDefaultValues() {
    RateLimit rateLimit = RateLimit.NO_LIMIT;

    assertThat(rateLimit.acquirePermit(new BasicHttpRequest("POST", "any"))).isTrue();
    assertThat(rateLimit.waitingTimeout()).isEqualTo(Duration.ZERO);
  }
}
