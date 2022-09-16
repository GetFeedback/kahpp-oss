package dev.vox.platform.kahpp.configuration.http.client;

import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimit;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimitHttpInterceptor;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RetryConfig;
import dev.vox.platform.kahpp.configuration.http.client.exception.ClientException;
import dev.vox.platform.kahpp.configuration.http.client.exception.RequestException;
import dev.vox.platform.kahpp.configuration.http.client.exception.ServerException;
import dev.vox.platform.kahpp.configuration.http.client.exception.TransferException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApiClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);

  private final HttpClient httpClient;
  private final String basePath;
  private final Map<String, String> defaultHeaders;

  private ApiClient(
      final HttpClient httpClient, final String basePath, final Map<String, String> headers) {
    this.basePath = basePath;
    this.httpClient = httpClient;
    this.defaultHeaders = headers;
  }

  public Response sendRequest(final String method, final String path) throws RequestException {
    return executeRequest(new Request(method, path, defaultHeaders));
  }

  public Response sendRequest(final String method, final String path, final String body)
      throws RequestException {
    return executeRequest(new Request(method, path, defaultHeaders, body));
  }

  private Response executeRequest(final Request request) throws RequestException {
    final HttpUriRequest httpRequest = buildHttpUriRequest(request);

    final Response response;
    try {
      response = transformHttpResponse(httpClient.execute(httpRequest));
    } catch (IOException exception) {
      LOGGER.error("Request {} failed because {}", httpRequest, exception.getMessage());
      throw new TransferException(exception, request);
    }

    if (between(response.getStatusCode(), 400, 499)) {
      throw new ClientException(request, response);
    }
    if (between(response.getStatusCode(), 500, 599)) {
      throw new ServerException(request, response);
    }

    return response;
  }

  private HttpUriRequest buildHttpUriRequest(final Request request) {
    final RequestBuilder builder =
        RequestBuilder.create(request.getMethod()).setUri(URI.create(basePath + request.getPath()));

    request.getHeaders().entrySet().stream()
        .map(this::mapEntryToHeader)
        .forEach(builder::addHeader);

    if (request.getBody().isPresent()) {
      builder.setEntity(new StringEntity(request.getBody().get(), ContentType.APPLICATION_JSON));
    }

    return builder.build();
  }

  private Header mapEntryToHeader(final Map.Entry<String, String> header) {
    return new BasicHeader(header.getKey(), header.getValue());
  }

  private static Response transformHttpResponse(final HttpResponse httpResponse)
      throws IOException {
    final String responseBody =
        httpResponse.getEntity() != null ? EntityUtils.toString(httpResponse.getEntity()) : null;
    HttpClientUtils.closeQuietly(httpResponse);

    return new Response(
        headersToMap(httpResponse.getAllHeaders()),
        httpResponse.getStatusLine().getStatusCode(),
        responseBody);
  }

  private static Map<String, String> headersToMap(final Header... headers) {
    return Arrays.stream(headers)
        .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
  }

  private static boolean between(int actualValue, int minValueInclusive, int maxValueInclusive) {
    return actualValue >= minValueInclusive && actualValue <= maxValueInclusive;
  }

  public static final class Builder {

    private static final Map<String, String> DEFAULT_HEADERS =
        new HashMap<>(Map.of(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString()));

    private final String basePath;

    private Map<String, String> headers;
    private RetryConfig retryConfig;

    private RequestConfig requestConfig;

    private RateLimit rateLimit;

    public Builder(final String basePath) {
      this.basePath = basePath;

      this.headers = DEFAULT_HEADERS;

      this.setRetryConfig(new RetryConfig())
          .setRequestConfig(0, 0)
          .setRateLimit(RateLimit.NO_LIMIT);
    }

    public Builder setHeaders(Map<String, String> headers) {
      this.headers = Map.copyOf(headers);
      return this;
    }

    public Builder setRetryConfig(RetryConfig retryConfig) {
      this.retryConfig = retryConfig;
      return this;
    }

    /**
     * Set the request configuration based on connect and socket timeouts
     *
     * <p>Timeout values of zero are interpreted as infinite timeouts. Negative values are
     * interpreted as undefined (system default if applicable).
     *
     * @param connectTimeoutMillis Determines the timeout in milliseconds until a connection is
     *     established.
     * @param socketTimeoutMillis Defines the socket timeout ({@code SO_TIMEOUT}) in milliseconds,
     *     which is the timeout for waiting for data or, put differently, a maximum period
     *     inactivity between two consecutive data packets).
     */
    public Builder setRequestConfig(final int connectTimeoutMillis, final int socketTimeoutMillis) {
      return setRequestConfig(
          RequestConfig.custom()
              .setConnectTimeout(connectTimeoutMillis)
              .setSocketTimeout(socketTimeoutMillis)
              .build());
    }

    private Builder setRequestConfig(RequestConfig requestConfig) {
      this.requestConfig = requestConfig;
      return this;
    }

    public Builder setRateLimit(RateLimit rateLimit) {
      this.rateLimit = rateLimit;
      return this;
    }

    public ApiClient build() {
      CloseableHttpClient httpClient =
          HttpClientBuilder.create()
              .useSystemProperties()
              .setRoutePlanner(new SystemDefaultRoutePlanner(null))
              .setDefaultRequestConfig(requestConfig)
              .setServiceUnavailableRetryStrategy(retryConfig.getServiceUnavailableRetryStrategy())
              .setRetryHandler(retryConfig.getHttpRequestRetryHandler())
              .addInterceptorFirst(new RateLimitHttpInterceptor(rateLimit))
              .build();

      return new ApiClient(httpClient, basePath, headers);
    }
  }
}
