package dev.vox.platform.kahpp.configuration.http.client.configuration;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;

public final class RateLimitBuilder {
  private final transient Integer requestsPerSecond;
  private transient Duration acquireTimeout;
  private transient Duration warmUpPeriod;

  public RateLimitBuilder(Integer requestsPerSecond) {
    this.requestsPerSecond = requestsPerSecond;
  }

  public RateLimitBuilder setAcquireTimeout(Duration acquireTimeout) {
    this.acquireTimeout = acquireTimeout;

    return this;
  }

  public RateLimitBuilder setWarmUpPeriod(Duration warmUpPeriod) {
    this.warmUpPeriod = warmUpPeriod;

    return this;
  }

  public RateLimit build() {
    final RateLimiter rateLimiter = buildRateLimiter();

    if (acquireTimeout != null) {
      return new SmoothRateLimit(rateLimiter, acquireTimeout);
    }

    return new SmoothRateLimit(rateLimiter);
  }

  private RateLimiter buildRateLimiter() {
    if (warmUpPeriod == null) {
      return RateLimiter.create(requestsPerSecond);
    }

    return RateLimiter.create(requestsPerSecond, warmUpPeriod);
  }
}
