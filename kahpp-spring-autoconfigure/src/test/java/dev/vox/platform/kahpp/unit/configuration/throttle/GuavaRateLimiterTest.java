package dev.vox.platform.kahpp.unit.configuration.throttle;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.throttle.GuavaRateLimiter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GuavaRateLimiterTest {
  @Test
  public void acquireIsRateLimited() {
    GuavaRateLimiter rateLimiter = new GuavaRateLimiter(10);

    long start = Instant.now().toEpochMilli();
    for (int i = 0; i < 10; i++) {
      rateLimiter.acquire();

      long checkpoint = Instant.now().toEpochMilli();
      assertThat(checkpoint - start).isLessThan(1000);
    }

    rateLimiter.acquire();

    long end = Instant.now().toEpochMilli();
    assertThat(end - start).isGreaterThanOrEqualTo(1000);
  }
}
