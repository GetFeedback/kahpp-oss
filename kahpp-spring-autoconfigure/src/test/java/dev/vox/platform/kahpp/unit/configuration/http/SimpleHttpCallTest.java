package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerRecordUpdate;
import dev.vox.platform.kahpp.configuration.http.SimpleHttpCall;
import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.integration.KaHPPMockServer;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.vavr.control.Either;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleHttpCallTest {

  public static final String HTTP_CALL_PATH = "/enrich";
  private static final String FIXTURE_KEY = "key";
  private static final String FIXTURE_VALUE = "value";

  private transient HttpCall httpCall;
  private ApiClient apiClient;

  private final JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private static final JsonMapper JSON_MAPPER = new JsonMapper();

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

    apiClient = httpClient.buildApiClient();
    httpCall =
        new SimpleHttpCall(
            "http_test",
            Map.of(
                "api",
                "suchNiceApi",
                "path",
                "enrich",
                "apiClient",
                apiClient,
                "responseHandler",
                ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE));
  }

  @Test
  void assureProvidedNameIsTheSameAsRetrieved() {
    assertThat(httpCall.getName()).isEqualTo("http_test");
  }

  @Test
  void happyPathCallReturning200AsHttpResponse() throws IOException {
    JsonNode key = getJsonNodeFromFile(FIXTURE_KEY);
    JsonNode value = getJsonNodeFromFile(FIXTURE_VALUE);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, value.toString(), 200, "{}");

    Either<Throwable, RecordAction> afterCall =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L));

    assertTrue(afterCall.isRight());
    assertThat(afterCall.get()).isExactlyInstanceOf(TransformRecord.class);
    assertThat(((TransformRecord) afterCall.get()).getDataSource().toString()).isEqualTo("{}");
  }

  @Test
  void test200WithInvalidJsonResponse() throws IOException {
    JsonNode key = getJsonNodeFromFile(FIXTURE_KEY);
    JsonNode value = getJsonNodeFromFile(FIXTURE_VALUE);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, value.toString(), 200, "{'bad': 'json'}");

    Either<Throwable, RecordAction> transformRecord =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L));

    assertThat(transformRecord.getLeft().getClass().getSimpleName())
        .isEqualTo("ResponseHandlerException");
  }

  @Test
  void test500StatusCodeResponses() throws IOException {
    JsonNode key = getJsonNodeFromFile(FIXTURE_KEY);
    JsonNode value = getJsonNodeFromFile(FIXTURE_VALUE);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, value.toString(), 500);

    Either<Throwable, RecordAction> transformRecord =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L));

    assertThat(transformRecord.getLeft().getClass().getSimpleName()).isEqualTo("ServerException");
  }

  @Test
  void test400StatusCodeResponses() throws IOException {
    JsonNode key = getJsonNodeFromFile(FIXTURE_KEY);
    JsonNode value = getJsonNodeFromFile(FIXTURE_VALUE);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, value.toString(), 400);

    Either<Throwable, RecordAction> transformRecord =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L));

    assertThat(transformRecord.getLeft().getClass().getSimpleName()).isEqualTo("ClientException");
  }

  private JsonNode getJsonNodeFromFile(String file) throws IOException {
    String keyString =
        Files.readString(
            Paths.get("src/test/resources/Fixtures/collection/collection_6/" + file + ".json"));
    return new ObjectMapper().readTree(keyString);
  }

  @Test
  void shouldReplacePathWithValue() throws IOException {

    httpCall =
        new SimpleHttpCall(
            "http_test",
            Map.of(
                "api",
                "suchNiceApi",
                "path",
                HTTP_CALL_PATH + "/${value.payload.id}",
                "apiClient",
                apiClient,
                "responseHandler",
                ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE));

    JsonNode key = getJsonNodeFromFile(FIXTURE_KEY);
    JsonNode value = getJsonNodeFromFile(FIXTURE_VALUE);

    KaHPPMockServer.mockHttpResponse(
        HTTP_CALL_PATH + "/" + value.get("payload").get("id").textValue(),
        value.toString(),
        200,
        "{}");

    Either<Throwable, RecordAction> afterCall =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L), jacksonRuntime);

    assertTrue(afterCall.isRight());
    assertThat(afterCall.get()).isExactlyInstanceOf(TransformRecord.class);
    assertThat(((TransformRecord) afterCall.get()).getDataSource().toString()).isEqualTo("{}");

    JsonNode extraKey = JSON_MAPPER.createObjectNode().put("extra", "record");
    JsonNode extraValue =
        JSON_MAPPER
            .createObjectNode()
            .set("payload", JSON_MAPPER.createObjectNode().put("id", "extraCall"));
    KaHPPMockServer.mockHttpResponse(
        HTTP_CALL_PATH + "/extraCall", extraValue.toString(), 200, "{}");

    Either<Throwable, RecordAction> extraCall =
        httpCall.call(KaHPPRecord.build(extraKey, extraValue, 1584352842124L), jacksonRuntime);
    assertTrue(extraCall.isRight());
    assertThat(extraCall.get()).isExactlyInstanceOf(TransformRecord.class);
    assertThat(((TransformRecord) extraCall.get()).getDataSource().toString()).isEqualTo("{}");
  }

  @Test
  void shouldEndCallIfPlaceholderValueNotExists() throws IOException {

    httpCall =
        new SimpleHttpCall(
            "http_test_with_placeholder",
            Map.of(
                "api",
                "suchNiceApi",
                "path",
                HTTP_CALL_PATH + "/${value.notExistsThisField}",
                "apiClient",
                apiClient,
                "responseHandler",
                ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE));

    JsonNode key = getJsonNodeFromFile(FIXTURE_KEY);
    JsonNode value = getJsonNodeFromFile(FIXTURE_VALUE);

    Either<Throwable, RecordAction> afterCall =
        httpCall.call(KaHPPRecord.build(key, value, 1584352842123L), jacksonRuntime);

    assertTrue(afterCall.isLeft());
    assertThat(afterCall.getLeft().getClass().getSimpleName()).isEqualTo("RuntimeException");
  }
}
