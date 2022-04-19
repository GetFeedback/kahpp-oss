package dev.vox.platform.kahpp.configuration.http.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.JsonBody.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.vox.platform.kahpp.configuration.http.client.configuration.RetryConfig;
import dev.vox.platform.kahpp.configuration.http.client.exception.ClientException;
import dev.vox.platform.kahpp.configuration.http.client.exception.RequestException;
import dev.vox.platform.kahpp.configuration.http.client.exception.ServerException;
import dev.vox.platform.kahpp.configuration.http.client.exception.TransferException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpHeaders;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpError;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

class ApiClientTest {

  private static final String REQUEST_PATH = "/request-path";
  private static final String URL_BASE_PATH = "http://localhost:9090";
  private static final String VALID_JSON = "{\"key\":\"value\"}";

  private static ApiClient defaultApiClient;
  private static ClientAndServer mockServer;
  private static MockServerClient mockServerClient;

  @BeforeAll
  static void createAClientInstance() {
    RetryConfig retryConfig = new RetryConfig(1, true, false, 20, 100, 1);
    Map<String, String> headers =
        Map.of(
            HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString(), HttpHeaders.ETAG, "etag");

    defaultApiClient =
        new ApiClient.Builder(URL_BASE_PATH)
            .setRequestConfig(100, 100)
            .setRetryConfig(retryConfig)
            .setHeaders(headers)
            .build();
    mockServer = startClientAndServer(9090);
    mockServerClient = new MockServerClient("localhost", 9090);
  }

  @AfterAll
  static void stopMockServer() {
    mockServerClient.stop();
    mockServer.stop();
  }

  @BeforeEach
  void setUp() {
    mockServerClient.reset();
  }

  @Test
  void theApiClientIsConfiguredWithNoHeaders() throws RequestException {
    mockServerClient
        .when(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(200));

    new ApiClient.Builder(URL_BASE_PATH)
        .setRequestConfig(100, 100)
        .build()
        .sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH);

    mockServerClient.verify(
        HttpRequest.request()
            .withHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.toString()),
        VerificationTimes.exactly(1));
  }

  @Test
  void theApiClientIsConfiguredWithConfigureHeaders() throws RequestException {
    mockServerClient
        .when(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(200));

    defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH);

    mockServerClient.verify(
        HttpRequest.request().withHeader(HttpHeaders.ETAG, "etag"), VerificationTimes.exactly(1));
  }

  @Test
  void theApiClientIsConfiguredWithCustomHeaders() throws RequestException {
    final Map<String, String> customHeaders =
        Map.of(
            HttpHeaders.ACCEPT_CHARSET,
            StandardCharsets.UTF_8.name(),
            HttpHeaders.ACCEPT_ENCODING,
            "gzip");
    final ApiClient clientWithCustomHeaders =
        new ApiClient.Builder(URL_BASE_PATH)
            .setRequestConfig(100, 100)
            .setHeaders(customHeaders)
            .build();

    mockServerClient
        .when(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(200));

    clientWithCustomHeaders.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH);

    mockServerClient.verify(
        HttpRequest.request()
            .withHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.name())
            .withHeader(HttpHeaders.ACCEPT_ENCODING, "gzip"),
        VerificationTimes.exactly(1));
  }

  @Test
  void itCanCreateAndSendARequestWithoutABody() throws RequestException {
    mockServerClient
        .when(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(200));

    defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH);

    mockServerClient.verify(
        HttpRequest.request().withMethod(HttpGet.METHOD_NAME).withPath(REQUEST_PATH),
        VerificationTimes.exactly(1));
  }

  @Test
  void itCanCreateAndSendARequestWithABodyFromString() throws RequestException {
    mockServerClient
        .when(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(201));

    defaultApiClient.sendRequest(HttpPost.METHOD_NAME, REQUEST_PATH, VALID_JSON);

    mockServerClient.verify(
        HttpRequest.request()
            .withMethod(HttpPost.METHOD_NAME)
            .withPath(REQUEST_PATH)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
            .withBody(VALID_JSON),
        VerificationTimes.exactly(1));
  }

  @Test
  void itCanReceiveAResponseWithoutABody() throws RequestException {
    mockServerClient
        .when(HttpRequest.request())
        .respond(HttpResponse.response().withStatusCode(204));

    final Response result = defaultApiClient.sendRequest(HttpDelete.METHOD_NAME, REQUEST_PATH);

    assertThat(result.getStatusCode()).isEqualTo(204);
    assertThat(result.getBody().isEmpty()).isTrue();
  }

  @Test
  void itCanReceiveAResponseWithABody() throws RequestException {
    mockServerClient
        .when(HttpRequest.request())
        .respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .withBody(json(VALID_JSON)));

    final Response result = defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH);

    assertThat(result.getStatusCode()).isEqualTo(200);
    assertThat(result.getBody().isPresent()).isTrue();
    assertThat(result.getBody().get()).isEqualToIgnoringWhitespace(VALID_JSON);
    assertThat(result.getHeaders())
        .contains(Map.entry(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()));
  }

  @Test
  void itThrowsClientExceptionOn4xxResponse() {
    mockServerClient
        .when(HttpRequest.request())
        .respond(
            HttpResponse.response()
                .withStatusCode(400)
                .withBody(VALID_JSON)
                .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString()));

    final ClientException exception =
        assertThrows(
            ClientException.class,
            () -> defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH));

    assertThat(exception.getRequest().isPresent()).isTrue();
    assertThat(exception.getResponse().isPresent()).isTrue();
  }

  @Test
  void itThrowsServerExceptionOn5xxResponse() {
    mockServerClient
        .when(HttpRequest.request())
        .respond(
            HttpResponse.response()
                .withStatusCode(503)
                .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
                .withBody(json(VALID_JSON)));

    final ServerException exception =
        assertThrows(
            ServerException.class,
            () -> defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH));

    assertThat(exception.getRequest().isPresent()).isTrue();
    assertThat(exception.getResponse().isPresent()).isTrue();
  }

  @Test
  void itThrowsTransferExceptionWhenNoResponseIsReceived() {
    mockServerClient.when(HttpRequest.request()).error(HttpError.error().withDropConnection(true));

    final TransferException exception =
        assertThrows(
            TransferException.class,
            () -> defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH));

    assertThat(exception.getRequest().isPresent()).isTrue();
    assertThat(exception.getResponse().isPresent()).isFalse();
    assertThat(exception.getCause()).isInstanceOf(NoHttpResponseException.class);
  }

  @Test
  void itThrowsTransferExceptionWhenTheRequestTimesOut() {
    mockServerClient
        .when(HttpRequest.request())
        .error(HttpError.error().withDelay(TimeUnit.MILLISECONDS, 1000L));

    final TransferException exception =
        assertThrows(
            TransferException.class,
            () -> defaultApiClient.sendRequest(HttpGet.METHOD_NAME, REQUEST_PATH));

    assertThat(exception.getRequest().isPresent()).isTrue();
    assertThat(exception.getResponse().isPresent()).isFalse();
    assertThat(exception.getCause()).isInstanceOf(SocketTimeoutException.class);
  }

  private static class ProcessableRequestBody {

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    @SuppressWarnings({"unused", "PMD.BeanMembersShouldSerialize"})
    @JsonProperty("key")
    private String key;

    ProcessableRequestBody(final String value) {
      this.key = value;
    }
  }

  private static class UnprocessableRequestBody {

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    @SuppressWarnings({"unused", "PMD.BeanMembersShouldSerialize"})
    private String key;

    UnprocessableRequestBody(final String value) {
      this.key = value;
    }
  }
}
