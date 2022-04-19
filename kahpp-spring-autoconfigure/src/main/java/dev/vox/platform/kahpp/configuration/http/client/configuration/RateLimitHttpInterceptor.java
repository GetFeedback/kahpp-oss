package dev.vox.platform.kahpp.configuration.http.client.configuration;

import java.io.IOException;
import java.net.ConnectException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

public final class RateLimitHttpInterceptor implements HttpRequestInterceptor {

  private final RateLimit rateLimit;

  public RateLimitHttpInterceptor(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  @Override
  public void process(HttpRequest request, HttpContext context) throws IOException {
    if (!rateLimit.acquirePermit(request)) {
      throw new ConnectException("API rate limit exceeded");
    }
  }
}
