package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.Connection;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.RateLimitConfig;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.Retries;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.RetriesForHttpStatus;
import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimit;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RetryConfig;
import dev.vox.platform.kahpp.configuration.http.client.configuration.SmoothRateLimit;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;

class HttpClientTest {

  private static final String HTTP_BASE_PATH = "http://my-dummy-api/";
  private static final int CONNECT_TIMEOUT_MILLIS = 1;
  private static final int SOCKET_TIMEOUT_MS = 5;
  protected static final HttpContext context = new BasicHttpContext();

  private final transient HttpClient simpleHttpClient =
      new HttpClient(
          HTTP_BASE_PATH,
          new Options(
              new Connection(SOCKET_TIMEOUT_MS, CONNECT_TIMEOUT_MILLIS),
              Map.of("x", "y"),
              null,
              null));

  @Test
  public void canBuild() {
    ApiClient apiClient = simpleHttpClient.buildApiClient();
    assertThat(apiClient).isNotNull();
  }

  @Test
  public void optionsHasCorrectData() {
    Options options = simpleHttpClient.getOptions();
    assertThat(options.getHeaders()).containsExactly(new SimpleEntry<>("x", "y"));
    assertThat(options.getConnection().getConnectTimeoutMillis()).isEqualTo(CONNECT_TIMEOUT_MILLIS);
    assertThat(options.getConnection().getSocketTimeoutMs()).isEqualTo(SOCKET_TIMEOUT_MS);
  }

  @Test
  public void optionsCreatesDefaultRetries() {
    Options options = simpleHttpClient.getOptions();
    assertThat(options.getRetries()).isInstanceOf(Retries.class);
  }

  @Test
  public void canBuildWithoutHeadersAndRateLimit() {
    final HttpClient httpClient =
        new HttpClient(
            HTTP_BASE_PATH,
            new Options(
                new Connection(SOCKET_TIMEOUT_MS, CONNECT_TIMEOUT_MILLIS), null, null, null));

    ApiClient apiClient = httpClient.buildApiClient();

    assertThat(apiClient).isNotNull();
  }

  @Test
  public void canBuildWithHeadersAndWithoutRateLimit() {
    final HttpClient httpClient =
        new HttpClient(
            HTTP_BASE_PATH,
            new Options(
                new Connection(SOCKET_TIMEOUT_MS, CONNECT_TIMEOUT_MILLIS),
                Map.of("x", "y"),
                null,
                null));

    ApiClient apiClient = httpClient.buildApiClient();

    assertThat(apiClient).isNotNull();
  }

  @Test
  public void canBuildWithHeadersAndRateLimit() {
    final HttpClient httpClient =
        new HttpClient(
            HTTP_BASE_PATH,
            new Options(
                new Connection(SOCKET_TIMEOUT_MS, CONNECT_TIMEOUT_MILLIS),
                Map.of("x", "y"),
                new RateLimitConfig(10, null),
                null));

    ApiClient apiClient = httpClient.buildApiClient();

    assertThat(apiClient).isNotNull();
  }

  @Test
  public void canBuildWithRetries() {
    final HttpClient httpClient =
        new HttpClient(
            HTTP_BASE_PATH,
            new Options(
                new Connection(SOCKET_TIMEOUT_MS, CONNECT_TIMEOUT_MILLIS),
                Map.of("x", "y"),
                null,
                new Retries(null, null, null, null, null, null, null)));

    assertThat(httpClient.buildApiClient()).isNotNull();
  }

  @Test
  public void rateLimitConfigShouldBuildARateLimitWithoutWarmUpPeriod() {
    final RateLimitConfig rateLimitConfig = new RateLimitConfig(10, null);
    final RateLimit rateLimit = rateLimitConfig.build();

    assertThat(rateLimit).isInstanceOf(RateLimit.class);
    assertThat(rateLimit).isInstanceOf(SmoothRateLimit.class);
  }

  @Test
  public void rateLimitConfigShouldBuildARateLimitWithWarmUpPeriod() {
    final RateLimitConfig rateLimitConfig = new RateLimitConfig(10, 2000);
    final RateLimit rateLimit = rateLimitConfig.build();

    assertThat(rateLimit).isInstanceOf(RateLimit.class);
    assertThat(rateLimit).isInstanceOf(SmoothRateLimit.class);
  }

  @Test
  public void retriesBuildUsesDefaults() {
    final Retries retries = new Retries(null, null, null, null, null, null, null);
    final RetryConfig config = retries.build();
    final ServiceUnavailableRetryStrategy strategy = config.getServiceUnavailableRetryStrategy();

    assertThat(config).isInstanceOf(RetryConfig.class);
    assertThat(strategy.retryRequest(createResponse(429), 10, context)).isTrue();
    assertThat(strategy.retryRequest(createResponse(429), 11, context)).isFalse();
    assertThat(strategy.retryRequest(createResponse(500), 3, context)).isTrue();
    assertThat(strategy.retryRequest(createResponse(500), 4, context)).isFalse();
    assertThat(strategy.retryRequest(createResponse(599), 3, context)).isTrue();
    assertThat(strategy.retryRequest(createResponse(599), 4, context)).isFalse();
  }

  @Test
  public void retriesBuildUsesValues() {
    final List<RetriesForHttpStatus> statusCodes =
        List.of(
            new RetriesForHttpStatus(429, null, null, 5),
            new RetriesForHttpStatus(null, 500, 599, null));
    final Retries retries = new Retries(statusCodes, 5, null, null, 10, 100, 2);
    final RetryConfig config = retries.build();
    final ServiceUnavailableRetryStrategy strategy = config.getServiceUnavailableRetryStrategy();

    assertThat(config).isInstanceOf(RetryConfig.class);
    assertThat(strategy.retryRequest(createResponse(429), 5, context)).isTrue();
    assertThat(strategy.retryRequest(createResponse(429), 6, context)).isFalse();
    assertThat(strategy.retryRequest(createResponse(500), 1, context)).isTrue();
    assertThat(strategy.retryRequest(createResponse(599), 1, context)).isTrue();
    assertThat(strategy.retryRequest(createResponse(600), 1, context)).isFalse();
  }

  @Test
  public void retriesBuildsWithoutConnectionRetries() {
    final Retries retries = new Retries(null, 0, null, null, null, null, null);
    final RetryConfig config = retries.build();
    final HttpRequestRetryHandler handler = config.getHttpRequestRetryHandler();

    assertThat(handler).isInstanceOf(HttpRequestRetryHandler.class);
    assertThat(handler.retryRequest(new IOException(), 1, context)).isFalse();
  }

  protected HttpResponse createResponse(int statusCode) {
    return new BasicHttpResponse(new ProtocolVersion("http", 2, 0), statusCode, "");
  }
}
