package dev.vox.platform.kahpp.configuration.http.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import dev.vox.platform.kahpp.configuration.http.client.configuration.RateLimitBuilder;
import dev.vox.platform.kahpp.configuration.http.client.exception.TransferException;
import java.time.Duration;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

class RateLimitBuilderTest {

  private static final String REQUEST_PATH = "/request-path";
  private static final String URL_BASE_PATH = "http://localhost:9090";
  private static final String VALID_JSON = "{\"key\":\"value\"}";

  private static ClientAndServer mockServer;
  private static MockServerClient mockServerClient;

  @BeforeAll
  static void createAClientInstance() {
    mockServer = startClientAndServer(9090);
    mockServerClient = new MockServerClient("localhost", 9090);
  }

  @BeforeEach
  void setUp() {
    mockServerClient.reset();
  }

  @AfterAll
  static void stopMockServer() {
    mockServerClient.stop();
    mockServer.stop();
  }

  @Test
  void shouldMakeOnlyOneApiCallDueRateLimit() {
    mockServerClient
        .when(HttpRequest.request(), Times.once())
        .respond(HttpResponse.response().withStatusCode(200));

    final ApiClient apiClient =
        new ApiClient.Builder(URL_BASE_PATH)
            .setRateLimit(new RateLimitBuilder(1).setAcquireTimeout(Duration.ZERO).build())
            .build();

    assertDoesNotThrow(() -> apiClient.sendRequest(HttpPost.METHOD_NAME, REQUEST_PATH, VALID_JSON));
    assertThrows(
        TransferException.class,
        () -> apiClient.sendRequest(HttpPost.METHOD_NAME, REQUEST_PATH, VALID_JSON),
        "API rate limit exceeded");

    mockServerClient.verify(
        HttpRequest.request()
            .withMethod(HttpPost.METHOD_NAME)
            .withPath(REQUEST_PATH)
            .withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())
            .withBody(VALID_JSON),
        VerificationTimes.exactly(1));
  }
}
