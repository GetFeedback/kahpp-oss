package dev.vox.platform.kahpp.configuration.http.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimit;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimitHttpInterceptor;
import java.net.ConnectException;
import java.time.Duration;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.jupiter.api.Test;

class RateLimitHttpInterceptorTest {
  @Test
  void shouldThrowHttpCallExceptionWhenRateLimitIsOver() {
    final RateLimitHttpInterceptor rateLimitHttpInterceptor =
        new RateLimitHttpInterceptor(
            new RateLimit() {
              @Override
              public boolean acquirePermit(HttpRequest httpRequest) {
                return false;
              }

              @Override
              public Duration waitingTimeout() {
                return null;
              }
            });

    assertThrows(
        ConnectException.class,
        () ->
            rateLimitHttpInterceptor.process(
                new BasicHttpRequest("POST", "http://mock.local/"), new BasicHttpContext()),
        "API rate limit exceeded");
  }

  @Test
  void shouldMakeACallWhenRateLimitIsNotOver() {

    final RateLimitHttpInterceptor rateLimitHttpInterceptor =
        new RateLimitHttpInterceptor(
            new RateLimit() {
              @Override
              public boolean acquirePermit(HttpRequest httpRequest) {
                return true;
              }

              @Override
              public Duration waitingTimeout() {
                return null;
              }
            });

    assertDoesNotThrow(
        () ->
            rateLimitHttpInterceptor.process(
                new BasicHttpRequest("POST", "http://mock.local/"), new BasicHttpContext()));
  }
}
