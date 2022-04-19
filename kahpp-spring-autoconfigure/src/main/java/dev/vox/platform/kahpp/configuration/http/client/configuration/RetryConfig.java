package dev.vox.platform.kahpp.configuration.http.client.configuration;

import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;

public class RetryConfig {

  private static final int DEFAULT_CONNECTION_RETRY_COUNT = 3;
  private static final boolean DEFAULT_CONNECTION_RETRY_ENABLED = true;
  private static final boolean DEFAULT_CONNECTION_RETRY_ON_TIMEOUT = false;
  private static final int DEFAULT_STATUS_CODE_RETRY_TIME_SEED = 50;
  private static final int DEFAULT_STATUS_CODE_RETRY_TIME_MAX = 1000;
  private static final int DEFAULT_STATUS_CODE_RETRY_MEMORY = 10;

  private final int connectionRetryCount;
  private final boolean connectionRetryEnabled;
  private final boolean connectionRetryOnTimeout;
  private final int statusCodeRetryTimeSeedInMs;
  private final int statusCodeRetryTimeCapInMs;
  private final int statusCodeRetryMemory;
  private final Map<BackoffRetryStrategy.HttpStatusCodeRange, Integer> maxRetriesPerStatusCode =
      new HashMap<>();

  /** Creates a RetryConfig instance with the default values defined as constants in this class */
  public RetryConfig() {
    this(
        DEFAULT_CONNECTION_RETRY_COUNT,
        DEFAULT_CONNECTION_RETRY_ENABLED,
        DEFAULT_STATUS_CODE_RETRY_TIME_SEED,
        DEFAULT_STATUS_CODE_RETRY_TIME_MAX,
        DEFAULT_STATUS_CODE_RETRY_MEMORY);
  }

  /**
   * @param connectionRetryCount The amount of times to retry when the connection fails
   * @param connectionRetryEnabled true if it's OK to retry non-idempotent requests that have been
   *     sent see: {@link DefaultHttpRequestRetryHandler#DefaultHttpRequestRetryHandler(int,
   *     boolean)}
   * @param statusCodeRetryTimeSeedInMs see {@link BackoffRetryStrategy#BackoffRetryStrategy}
   * @param statusCodeRetryTimeCapInMs see {@link BackoffRetryStrategy#BackoffRetryStrategy}
   * @param statusCodeRetryMemory see {@link BackoffRetryStrategy#BackoffRetryStrategy}
   */
  private RetryConfig(
      final int connectionRetryCount,
      final boolean connectionRetryEnabled,
      final int statusCodeRetryTimeSeedInMs,
      final int statusCodeRetryTimeCapInMs,
      final int statusCodeRetryMemory) {
    this(
        connectionRetryCount,
        connectionRetryEnabled,
        DEFAULT_CONNECTION_RETRY_ON_TIMEOUT,
        statusCodeRetryTimeSeedInMs,
        statusCodeRetryTimeCapInMs,
        statusCodeRetryMemory);
  }

  /**
   * @param connectionRetryCount The amount of times to retry when the connection fails
   * @param connectionRetryEnabled true if it's OK to retry non-idempotent requests that have been
   *     sent see: {@link DefaultHttpRequestRetryHandler#DefaultHttpRequestRetryHandler(int,
   *     boolean)}
   * @param connectionRetryOnTimeout true if it should retry timeout errors see {@link
   *     ApiClient.Builder#setRequestConfig(int, int) }
   * @param statusCodeRetryTimeSeedInMs see {@link BackoffRetryStrategy#BackoffRetryStrategy}
   * @param statusCodeRetryTimeCapInMs see {@link BackoffRetryStrategy#BackoffRetryStrategy}
   * @param statusCodeRetryMemory see {@link BackoffRetryStrategy#BackoffRetryStrategy}
   */
  public RetryConfig(
      final int connectionRetryCount,
      final boolean connectionRetryEnabled,
      final boolean connectionRetryOnTimeout,
      final int statusCodeRetryTimeSeedInMs,
      final int statusCodeRetryTimeCapInMs,
      final int statusCodeRetryMemory) {
    this.connectionRetryCount = connectionRetryCount;
    this.connectionRetryEnabled = connectionRetryEnabled;
    this.connectionRetryOnTimeout = connectionRetryOnTimeout;
    this.statusCodeRetryTimeSeedInMs = statusCodeRetryTimeSeedInMs;
    this.statusCodeRetryTimeCapInMs = statusCodeRetryTimeCapInMs;
    this.statusCodeRetryMemory = statusCodeRetryMemory;
  }

  public RetryConfig addRetryForStatusCode(int exact, int retries) {
    maxRetriesPerStatusCode.put(new BackoffRetryStrategy.HttpStatusCodeRange(exact), retries);

    return this;
  }

  public RetryConfig addRetryForStatusCode(int start, int inclusiveEnd, int retries) {
    maxRetriesPerStatusCode.put(
        new BackoffRetryStrategy.HttpStatusCodeRange(start, inclusiveEnd), retries);

    return this;
  }

  public ServiceUnavailableRetryStrategy getServiceUnavailableRetryStrategy() {
    return new BackoffRetryStrategy(
        maxRetriesPerStatusCode,
        statusCodeRetryTimeSeedInMs,
        statusCodeRetryMemory,
        statusCodeRetryTimeCapInMs);
  }

  public HttpRequestRetryHandler getHttpRequestRetryHandler() {
    if (connectionRetryOnTimeout) {
      return new TimeoutAwareHttpRequestRetryHandler(connectionRetryCount, connectionRetryEnabled);
    }

    return new DefaultHttpRequestRetryHandler(connectionRetryCount, connectionRetryEnabled);
  }
}
