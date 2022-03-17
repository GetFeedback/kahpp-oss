package dev.vox.platform.kahpp.configuration.throttle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuavaRateLimiter implements RateLimiter {
  private static final Logger LOGGER = LoggerFactory.getLogger(GuavaRateLimiter.class);
  private final transient com.google.common.util.concurrent.RateLimiter rateLimiter;

  public GuavaRateLimiter(int recordsPerSecond) {
    this.rateLimiter = com.google.common.util.concurrent.RateLimiter.create(recordsPerSecond);
  }

  @Override
  public void acquire() {
    LOGGER.debug("Attempting to acquire permit");

    double delay = rateLimiter.acquire();
    if (delay > 0) {
      LOGGER.debug(
          "Waited {} seconds due to defined rate limit of `{}`", delay, rateLimiter.getRate());
    }

    LOGGER.debug("Acquired permit");
  }
}
