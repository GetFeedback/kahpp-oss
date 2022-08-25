package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.http.HandleByStatusCode;
import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerException;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerRecordRoute;
import dev.vox.platform.kahpp.configuration.http.client.exception.ClientException;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.configuration.util.Range;
import dev.vox.platform.kahpp.integration.KaHPPMockServer;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.vavr.control.Either;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseHandlerRecordRouteTest {

  private transient HandleByStatusCode httpCall;

  private static final TopicEntry.TopicIdentifier topic = new TopicEntry.TopicIdentifier("topic");

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeAll
  static void setupMockServer() {
    KaHPPMockServer.initServer();
  }

  @AfterAll
  static void stopMockServer() {
    KaHPPMockServer.closeServer();
  }

  @BeforeEach
  void init() {
    HttpClient.Options.Connection connection = new HttpClient.Options.Connection(500, 500);

    HttpClient.Options options =
        new HttpClient.Options(connection, Map.of("Accept-version", "v1"), null, null);

    HttpClient httpClient =
        new HttpClient(
            String.format("http://localhost:%s/", KaHPPMockServer.getLocalPort()), options);

    httpCall =
        new HandleByStatusCode(
            "http_test",
            Map.of(
                "api",
                "suchNiceApi",
                "path",
                "enrich",
                "apiClient",
                httpClient.buildApiClient(),
                "responseHandlers",
                Map.of(
                    new Range(400, 500),
                    new ResponseHandlerRecordRoute(Collections.singleton(topic)))));
  }

  @Test
  void shouldRouteToTopicIfTheStatusCodeIsInRange() {
    JsonNode key = objectMapper.createObjectNode().put("key", 1);
    JsonNode value = objectMapper.createObjectNode().put("value", "foo");

    KaHPPMockServer.mockHttpResponse("/enrich", value.toString(), 410, "{\"foo\":\"bar\"}");

    Either<Throwable, RecordAction> afterCall =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L));

    assertTrue(afterCall.isLeft());
    assertThat(afterCall.getLeft()).isInstanceOf(ClientException.class);
    assertThat(((ClientException) afterCall.getLeft()).getResponse().get().getStatusCode())
        .isEqualTo(410);
    assertThat(((ClientException) afterCall.getLeft()).getResponse().get().getBody())
        .contains("""
            {"foo":"bar"}""");
  }

  @Test
  void shouldNotRouteToTopicIfTheStatusCodeIsNotInRange() {
    JsonNode key = objectMapper.createObjectNode().put("key", 1);
    JsonNode value = objectMapper.createObjectNode().put("value", "foo");

    KaHPPMockServer.mockHttpResponse("/enrich", value.toString(), 200, "{\"foo\":\"bar\"}");

    Either<Throwable, RecordAction> afterCall =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L));

    assertTrue(afterCall.isLeft());
    assertThat(afterCall.getLeft()).isInstanceOf(ResponseHandlerException.class);
    assertThat((ResponseHandlerException) afterCall.getLeft())
        .hasMessage("No responseHandler matched with status code `200`");
  }
}
