package dev.vox.platform.kahpp.configuration.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimit;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimitBuilder;
import java.time.Duration;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SmoothRateLimitTest {

  private transient HttpRequest httpRequest;

  @BeforeEach
  void setUp() {
    httpRequest = new BasicHttpRequest("POST", "htp://foo.bar");
  }

  @Test
  void smoothBusrtyShouldImplementRateLimit() {
    assertThat(new RateLimitBuilder(1).build()).isInstanceOf(RateLimit.class);
  }

  @Test
  void smoothBusrtyWithWarmupShouldImplementRateLimit() {
    assertThat(new RateLimitBuilder(1).setWarmUpPeriod(Duration.ofSeconds(2)).build())
        .isInstanceOf(RateLimit.class);
  }

  @Test
  void rateLimitAfterTimeoutShouldNotAcquire() {
    final RateLimit zeroTimeoutHttpRateLimit =
        new RateLimitBuilder(1).setAcquireTimeout(Duration.ZERO).build();

    assertThat(zeroTimeoutHttpRateLimit.waitingTimeout()).isEqualByComparingTo(Duration.ZERO);
    assertThat(zeroTimeoutHttpRateLimit.acquirePermit(httpRequest)).isTrue();
    assertThat(zeroTimeoutHttpRateLimit.acquirePermit(httpRequest)).isFalse();
  }

  @Test
  void rateLimitAfterWaitingShouldAcquire() {
    final RateLimit zeroTimeoutHttpRateLimit = new RateLimitBuilder(1).build();

    assertThat(zeroTimeoutHttpRateLimit.acquirePermit(httpRequest)).isTrue();
    assertThat(zeroTimeoutHttpRateLimit.acquirePermit(httpRequest)).isTrue();
  }
}
