package dev.vox.platform.kahpp.configuration.http;

import com.usabilla.retryableapiclient.ApiClient;
import com.usabilla.retryableapiclient.RateLimit;
import com.usabilla.retryableapiclient.ratelimit.RateLimitBuilder;
import com.usabilla.retryableapiclient.retry.RetryConfig;
import dev.vox.platform.kahpp.configuration.http.validation.RetryableHttpStatusCodes;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.URL;

public final class HttpClient {
  @NotBlank @URL private final transient String basePath;
  @Valid private final transient Options options;

  public HttpClient(String basePath, Options options) {
    this.basePath = basePath;
    this.options = options;
  }

  public ApiClient buildApiClient() {
    final ApiClient.Builder apiBuilder = new ApiClient.Builder(basePath);

    apiBuilder.setRequestConfig(
        options.getConnection().getConnectTimeoutMillis(),
        options.getConnection().getSocketTimeoutMs());
    if (options.getHeaders() != null) {
      apiBuilder.setHeaders(options.getHeaders());
    }
    if (options.getRateLimit() != null) {
      apiBuilder.setRateLimit(options.getRateLimit().build());
    }
    apiBuilder.setRetryConfig(options.getRetries().build());

    return apiBuilder.build();
  }

  public Options getOptions() {
    return options;
  }

  public static class Options {
    @Valid @NotNull private final Connection connection;
    private final Map<@NotBlank String, @NotBlank String> headers;
    private final RateLimitConfig rateLimit;
    @Valid private final Retries retries;

    public Options(
        Connection connection,
        Map<String, String> headers,
        RateLimitConfig rateLimit,
        Retries retries) {
      this.connection = connection;
      this.headers = headers != null ? Map.copyOf(headers) : null;
      this.rateLimit = rateLimit;
      this.retries =
          retries != null ? retries : new Retries(null, null, null, null, null, null, null);
    }

    public Connection getConnection() {
      return connection;
    }

    public Map<String, String> getHeaders() {
      return headers != null ? Collections.unmodifiableMap(headers) : null;
    }

    public RateLimitConfig getRateLimit() {
      return rateLimit;
    }

    public Retries getRetries() {
      return retries;
    }

    public static class Connection {
      @Positive private final Integer socketTimeoutMs;
      @Positive private final Integer connectTimeoutMillis;

      public Connection(Integer socketTimeoutMs, Integer connectTimeoutMillis) {
        this.socketTimeoutMs = socketTimeoutMs;
        this.connectTimeoutMillis = connectTimeoutMillis;
      }

      public Integer getSocketTimeoutMs() {
        return socketTimeoutMs;
      }

      public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
      }
    }

    /** In the future we can use this class as a factory to build N RateLimit types */
    public static final class RateLimitConfig {
      @Positive private final transient Integer requestsPerSecond;
      @PositiveOrZero private final transient Integer warmUpMillis;

      public RateLimitConfig(
          @Positive Integer requestsPerSecond, @PositiveOrZero Integer warmUpMillis) {
        this.requestsPerSecond = requestsPerSecond;
        this.warmUpMillis = warmUpMillis;
      }

      public RateLimit build() {
        final RateLimitBuilder rateLimitBuilder = new RateLimitBuilder(requestsPerSecond);
        if (warmUpMillis != null) {
          rateLimitBuilder.setWarmUpPeriod(Duration.ofMillis(warmUpMillis));
        }

        return rateLimitBuilder.build();
      }
    }

    public static final class Retries {
      @Positive private final transient Integer connectionRetryCount;
      private final transient Boolean retryIdempotentRequests;
      private final transient Boolean retryOnTimeout;
      @Positive private final transient Integer statusCodeRetryTimeSeedInMs;
      @Positive private final transient Integer statusCodeRetryTimeCapInMs;
      @Positive private final transient Integer statusCodeRetryMemory;
      private final transient List<@NotNull @Valid RetriesForHttpStatus> statusCodes;

      public Retries(
          List<@NotNull @Valid RetriesForHttpStatus> statusCodes,
          @PositiveOrZero Integer connectionRetryCount,
          Boolean retryIdempotentRequests,
          Boolean retryOnTimeout,
          @Positive Integer statusCodeRetryTimeSeedInMs,
          @Positive Integer statusCodeRetryTimeCapInMs,
          @Positive Integer statusCodeRetryMemory) {
        this.statusCodes =
            statusCodes != null
                ? statusCodes
                : List.of(
                    new RetriesForHttpStatus(null, 500, 599, 3),
                    new RetriesForHttpStatus(429, null, null, 10));
        this.connectionRetryCount = connectionRetryCount != null ? connectionRetryCount : 3;
        this.retryIdempotentRequests =
            retryIdempotentRequests != null ? retryIdempotentRequests : true;
        this.retryOnTimeout = retryOnTimeout != null ? retryOnTimeout : false;
        this.statusCodeRetryTimeSeedInMs =
            statusCodeRetryTimeSeedInMs != null ? statusCodeRetryTimeSeedInMs : 50;
        this.statusCodeRetryTimeCapInMs =
            statusCodeRetryTimeCapInMs != null ? statusCodeRetryTimeCapInMs : 1000;
        this.statusCodeRetryMemory = statusCodeRetryMemory != null ? statusCodeRetryMemory : 25;
      }

      public RetryConfig build() {
        RetryConfig config =
            new RetryConfig(
                connectionRetryCount,
                retryIdempotentRequests,
                retryOnTimeout,
                statusCodeRetryTimeSeedInMs,
                statusCodeRetryTimeCapInMs,
                statusCodeRetryMemory);

        statusCodes.forEach(
            statusCodeConfig -> {
              statusCodeConfig.addHttpStatusCodeRetry(config);
            });

        return config;
      }
    }

    @RetryableHttpStatusCodes
    public static final class RetriesForHttpStatus {
      @Positive private final transient Integer statusCode;
      @Positive private final transient Integer statusCodeStart;
      @Positive private final transient Integer statusCodeInclusiveEnd;
      @Positive private final transient Integer retries;

      public RetriesForHttpStatus(
          @Positive Integer statusCode,
          @Positive Integer statusCodeStart,
          @Positive Integer statusCodeInclusiveEnd,
          @Positive Integer retries) {
        this.statusCode = statusCode;
        this.statusCodeStart = statusCodeStart;
        this.statusCodeInclusiveEnd = statusCodeInclusiveEnd;
        this.retries = retries != null ? retries : 3;
      }

      public void addHttpStatusCodeRetry(RetryConfig config) {
        if (statusCode != null) {
          config.addRetryForStatusCode(statusCode, retries);
          return;
        }

        config.addRetryForStatusCode(statusCodeStart, statusCodeInclusiveEnd, retries);
      }

      public Integer getStatusCode() {
        return statusCode;
      }

      public Integer getStatusCodeStart() {
        return statusCodeStart;
      }

      public Integer getStatusCodeInclusiveEnd() {
        return statusCodeInclusiveEnd;
      }
    }
  }
}
