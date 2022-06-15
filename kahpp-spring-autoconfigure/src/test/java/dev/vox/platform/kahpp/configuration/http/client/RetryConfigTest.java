package dev.vox.platform.kahpp.configuration.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.client.configuration.BackoffRetryStrategy;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RetryConfig;
import dev.vox.platform.kahpp.configuration.http.client.configuration.TimeoutAwareHttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.junit.jupiter.api.Test;

class RetryConfigTest {

  @Test
  void canBuildRetryStrategy() {
    RetryConfig retryConfig =
        new RetryConfig(3, false, false, 10, 100, 3)
            .addRetryForStatusCode(429, 10)
            .addRetryForStatusCode(500, 599, 1);

    assertThat(retryConfig.getHttpRequestRetryHandler())
        .isInstanceOf(DefaultHttpRequestRetryHandler.class);

    ServiceUnavailableRetryStrategy retryStrategy =
        retryConfig.getServiceUnavailableRetryStrategy();
    assertThat(retryStrategy).isNotNull();
    assertThat(retryStrategy).isInstanceOf(BackoffRetryStrategy.class);
  }

  @Test
  void canBuildRetryStrategyWithTimeoutAwareHttpRequestRetryHandler() {
    RetryConfig retryConfig =
        new RetryConfig(3, false, true, 10, 100, 3)
            .addRetryForStatusCode(429, 10)
            .addRetryForStatusCode(500, 599, 1);

    assertThat(retryConfig.getHttpRequestRetryHandler())
        .isInstanceOf(TimeoutAwareHttpRequestRetryHandler.class);
  }
}
