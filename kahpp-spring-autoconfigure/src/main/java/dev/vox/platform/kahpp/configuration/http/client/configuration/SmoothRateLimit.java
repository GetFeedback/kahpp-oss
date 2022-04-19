package dev.vox.platform.kahpp.configuration.http.client.configuration;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class SmoothRateLimit implements RateLimit {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmoothRateLimit.class);

  private static final Duration RATE_LIMIT_WAITING_TIMEOUT = Duration.ofSeconds(2L);

  private final transient RateLimiter rateLimiter;
  private final transient Duration waitingTimeout;

  SmoothRateLimit(RateLimiter rateLimiter) {
    this(rateLimiter, RATE_LIMIT_WAITING_TIMEOUT);
  }

  SmoothRateLimit(RateLimiter rateLimiter, Duration waitingTimeout) {
    this.waitingTimeout = waitingTimeout;

    this.rateLimiter = rateLimiter;
  }

  @Override
  public boolean acquirePermit(HttpRequest httpRequest) {
    final boolean acquired = rateLimiter.tryAcquire(Duration.ZERO);
    if (acquired) {
      return true;
    }

    LOGGER.info(
        "{}: request waiting due to defined rate limit of `{}`",
        httpRequest.getRequestLine().getUri(),
        rateLimiter.getRate());

    boolean acquiredAfterWaiting = rateLimiter.tryAcquire(waitingTimeout);
    if (acquiredAfterWaiting) {
      return true;
    }

    LOGGER.warn(
        "{}: request reached the waiting timeout of {}",
        httpRequest.getRequestLine().getUri(),
        waitingTimeout);

    return false;
  }

  @Override
  public Duration waitingTimeout() {
    return waitingTimeout;
  }
}
