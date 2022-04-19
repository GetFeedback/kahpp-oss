package dev.vox.platform.kahpp.configuration.http.client.configuration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpResponse;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

/**
 * Strategy to retry responses with specific status codes
 *
 * <p>Calculates how long the request should wait to retry based on a time seed and the last
 * requests that failed
 */
public class BackoffRetryStrategy implements ServiceUnavailableRetryStrategy {

  private final Map<HttpStatusCodeRange, Integer> maxRetriesPerStatusCode;

  private final int backoffSeed;

  private int backoffCap;

  private final MovingAverage executionCountAverage;

  /**
   * @param maxRetriesPerStatusCode Maximum number of allowed retries per status code range
   * @param backoffSeed A seed in milliseconds used to calculate the backoff time
   * @param backoffMemory The amount of requests that contribute to the backoff time calculation
   * @param backoffCap The cap of the backoff time
   */
  public BackoffRetryStrategy(
      final Map<HttpStatusCodeRange, Integer> maxRetriesPerStatusCode,
      final int backoffSeed,
      final int backoffMemory,
      final int backoffCap) {
    this.maxRetriesPerStatusCode = Map.copyOf(maxRetriesPerStatusCode);
    this.backoffSeed = Args.positive(backoffSeed, "Backoff seed");
    this.executionCountAverage = new MovingAverage(backoffMemory);
    this.backoffCap = Args.positive(backoffCap, "Backoff cap");
  }

  @Override
  public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
    executionCountAverage.add(executionCount);

    return executionCount <= retriesForStatusCode(response.getStatusLine().getStatusCode());
  }

  private int retriesForStatusCode(int code) {
    for (Map.Entry<HttpStatusCodeRange, Integer> statusCodeWithRetries :
        maxRetriesPerStatusCode.entrySet()) {
      if (statusCodeWithRetries.getKey().contains(code)) {
        return statusCodeWithRetries.getValue();
      }
    }

    return 0;
  }

  @Override
  public long getRetryInterval() {
    double intervalByMovingAverage =
        backoffSeed * Math.pow(2, executionCountAverage.getAverage() - 1);

    return (long) Math.min(intervalByMovingAverage, backoffCap);
  }

  public static class HttpStatusCodeRange {
    private int start;
    private int inclusiveEnd;

    public HttpStatusCodeRange(int exact) {
      this.start = exact;
      this.inclusiveEnd = exact;
    }

    public HttpStatusCodeRange(int start, int inclusiveEnd) {
      this.start = start;
      this.inclusiveEnd = inclusiveEnd;
    }

    public boolean contains(int code) {
      return code >= start && code <= inclusiveEnd;
    }
  }

  private static class MovingAverage {

    private final double size;
    private double total;
    private AtomicInteger index = new AtomicInteger(0);
    private final double[] samples;

    public MovingAverage(int size) {
      this.size = size;
      this.samples = new double[size];

      for (int i = 0; i < size; i++) {
        samples[i] = 1;
      }
      total = size;
    }

    public void add(int x) {
      int newIndex = Math.min(index.incrementAndGet(), (int) size);
      int currentIndex = newIndex - 1;

      total -= samples[currentIndex];
      samples[currentIndex] = x;
      total += x;

      if (newIndex >= size) { // cheaper than modulus
        index.set(0);
      }
    }

    public double getAverage() {
      return total / size;
    }
  }
}
