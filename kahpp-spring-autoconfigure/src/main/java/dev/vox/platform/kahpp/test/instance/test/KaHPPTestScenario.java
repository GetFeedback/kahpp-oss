package dev.vox.platform.kahpp.test.instance.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.streams.test.TestRecord;

/**
 * This is a simple registry for all the resources necessary to run a KaHPP Functional test
 * scenario, all the fixtures paths, from Kafka Records to Pact Interactions. No Test time logic
 * will be implemented here, for that refer to the tests themselves.
 */
public class KaHPPTestScenario {

  private final transient Path path;
  private final transient TestRecord<JsonNode, JsonNode> sourceRecord;
  private final transient Map<String, List<TestRecord<JsonNode, JsonNode>>> expectedRecordsPerSink =
      new HashMap<>();
  private final transient Map<String, Map<String, JsonNode>> expectedApiInteractions =
      new HashMap<>();

  private final transient ObjectNode pactSpecification =
      InstanceTestConfiguration.MAPPER
          .createObjectNode()
          .set(
              "pactSpecification",
              InstanceTestConfiguration.MAPPER.createObjectNode().put("version", "2.0.0"));

  /**
   * @param path The path which the scenario was found
   * @param sourceRecord A Source Record is the minimum requirement to have a Functional test
   *     running, as KaHPP might discard them via Filters or other Steps
   */
  public KaHPPTestScenario(Path path, KaHPPTestRecord sourceRecord) {
    this.path = path;
    this.sourceRecord = sourceRecord;
  }

  public void addExpectedRecords(String sink, List<TestRecord<JsonNode, JsonNode>> actualRecords) {
    this.expectedRecordsPerSink.put(sink, actualRecords);
  }

  public void addPactInteraction(
      String apiIdentifier, String pactInteractionIdentifier, JsonNode pactInteraction) {
    if (!this.expectedApiInteractions.containsKey(apiIdentifier)) {
      this.expectedApiInteractions.put(apiIdentifier, new HashMap<>());
    }
    ((ObjectNode) pactInteraction)
        .put("description", pactInteractionIdentifier)
        .set("metadata", pactSpecification);
    this.expectedApiInteractions.get(apiIdentifier).put(pactInteractionIdentifier, pactInteraction);
  }

  public void addPactInteractions(String apiIdentifier, Map<Path, JsonNode> pactInteractions) {
    pactInteractions.forEach(
        (pactInteractionPath, pactInteraction) -> {
          // The identifier should look like `1/api/my-dummy-api/foo-happy.json`
          String interactionIdentifier =
              this.path.getParent().getParent().relativize(pactInteractionPath).toString();
          this.addPactInteraction(apiIdentifier, interactionIdentifier, pactInteraction);
        });
  }

  public Map<String, Map<String, JsonNode>> getExpectedApiInteractions() {
    return Map.copyOf(expectedApiInteractions);
  }

  public Path getPath() {
    return path;
  }

  public TestRecord<JsonNode, JsonNode> getSourceRecord() {
    return sourceRecord;
  }

  public Map<String, List<TestRecord<JsonNode, JsonNode>>> getExpectedRecordsPerSink() {
    return Map.copyOf(expectedRecordsPerSink);
  }
}
