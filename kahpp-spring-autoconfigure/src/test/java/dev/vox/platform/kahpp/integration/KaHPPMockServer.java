package dev.vox.platform.kahpp.integration;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.IOException;
import java.net.ServerSocket;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

public class KaHPPMockServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KaHPPMockServer.class);

  private static final int HTTP_STATUS_CODE_SERVER_ERROR = 500;

  /*
   * This request should happen only once
   */
  private static final Integer REQUEST_WITHOUT_RETRIES = 1;

  /*
   * This request should happen 4 times: the first request and 3 more attempts
   */
  private static final Integer REQUEST_WITH_RETRIES = 4;

  private static ClientAndServer mockServer;

  public static void initServer() {
    mockServer = startClientAndServer(findAvailableRandomPort());
  }

  public static void closeServer() {
    mockServer.close();
  }

  public static void mockHttpResponse(String path, String body, int statusCode) {
    mockHttpResponse(path, body, statusCode, null);
  }

  public static void mockHttpResponse(
      String path, String body, int statusCode, String responseBody) {
    int requestTimes = REQUEST_WITHOUT_RETRIES;
    if (statusCode >= HTTP_STATUS_CODE_SERVER_ERROR) {
      requestTimes = REQUEST_WITH_RETRIES;
    }

    new MockServerClient("localhost", mockServer.getLocalPort())
        .when(
            HttpRequest.request().withMethod("POST").withPath(path).withBody(new JsonBody(body)),
            Times.exactly(requestTimes))
        .respond(HttpResponse.response().withStatusCode(statusCode).withBody(responseBody));
  }

  private static int findAvailableRandomPort() {
    int port = SocketUtils.findAvailableTcpPort();
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      return serverSocket.getLocalPort();
    } catch (IOException e) {
      LOGGER.error("Port no available");
      throw new RuntimeException("Port no available", e);
    }
  }

  public static int getLocalPort() {
    return mockServer != null ? mockServer.getLocalPort() : 0;
  }
}
