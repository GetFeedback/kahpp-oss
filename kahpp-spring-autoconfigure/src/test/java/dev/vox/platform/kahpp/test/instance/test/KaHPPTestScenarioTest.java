package dev.vox.platform.kahpp.test.instance.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.Test;

class KaHPPTestScenarioTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private KaHPPTestScenario loadDefaultTestScenario() throws URISyntaxException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL resource = classLoader.getResource("kahpp-test-scenario");
    Path kafkaFixturesPath = Path.of(resource.toURI());
    JsonNode key = objectMapper.createObjectNode().put("key", 1);
    JsonNode value = objectMapper.createObjectNode().put("value", "foo");
    TestRecord<JsonNode, JsonNode> testRecord = new TestRecord<JsonNode, JsonNode>(key, value);
    KaHPPTestRecord kaHPPTestRecord = KaHPPTestRecord.from(testRecord);
    return new KaHPPTestScenario(kafkaFixturesPath, kaHPPTestRecord);
  }

  @Test
  void addExpectedRecords() throws URISyntaxException {
    JsonNode key = objectMapper.createObjectNode().put("new-key", 2);
    JsonNode value = objectMapper.createObjectNode().put("new-value", "bar");
    TestRecord<JsonNode, JsonNode> testRecord = new TestRecord<>(key, value);

    KaHPPTestScenario kaHPPTestScenario = loadDefaultTestScenario();
    assertThat(kaHPPTestScenario.getPath()).isNotNull();
    assertThat(kaHPPTestScenario.getSourceRecord()).isNotNull();
    assertThat(kaHPPTestScenario.getPath().toString())
        .contains("/resources/test/kahpp-test-scenario");

    kaHPPTestScenario.addExpectedRecords("test-sink", Collections.singletonList(testRecord));
    assertThat(kaHPPTestScenario.getExpectedRecordsPerSink().get("test-sink")).contains(testRecord);
  }

  @Test
  void addPactInteraction() throws URISyntaxException {
    JsonNode interaction = objectMapper.createObjectNode().put("post", "hello/world");

    KaHPPTestScenario kaHPPTestScenario = loadDefaultTestScenario();
    kaHPPTestScenario.addPactInteraction("api-test", "api-interaction", interaction);
    kaHPPTestScenario.addPactInteraction("api-test", "api-interaction", interaction);
    assertThat(kaHPPTestScenario.getExpectedApiInteractions().get("api-test"))
        .containsEntry("api-interaction", interaction);
  }

  @Test
  void addPactInteractions() throws URISyntaxException {
    JsonNode interaction = objectMapper.createObjectNode().put("post", "hello/world");
    Path path = Path.of("/1/api/my-dummy-api/foo-happy.json");
    KaHPPTestScenario kaHPPTestScenario = loadDefaultTestScenario();
    kaHPPTestScenario.addPactInteractions("api-test", Map.of(path, interaction));
    assertThat(kaHPPTestScenario.getExpectedApiInteractions().get("api-test")).isNotNull();
  }
}
