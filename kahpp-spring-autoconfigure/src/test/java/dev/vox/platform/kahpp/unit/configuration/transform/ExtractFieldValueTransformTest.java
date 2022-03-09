package dev.vox.platform.kahpp.unit.configuration.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.transform.ExtractFieldValueTransform;
import dev.vox.platform.kahpp.configuration.transform.RecordTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtractFieldValueTransformTest {

  private static final String STEP_NAME = "name";
  private static final String CONFIG_FIELD = "field";
  private static final String FIELD_TO_EXTRACT = "toExtract";

  private static final JacksonRuntime JACKSON_RUNTIME = new JacksonRuntime();

  private transient RecordTransform recordTransform;

  @BeforeEach
  void setUp() {
    recordTransform =
        new ExtractFieldValueTransform(STEP_NAME, Map.of(CONFIG_FIELD, FIELD_TO_EXTRACT));
  }

  @Test
  void shouldExtractField() {
    final TransformRecord transformation =
        recordTransform.transform(
            JACKSON_RUNTIME,
            new MockProcessorContext(),
            KaHPPRecord.build(
                NullNode.getInstance(),
                json(String.format("{\"%s\":{\"nice\":\"value\"}}", FIELD_TO_EXTRACT)),
                1584352842123L));

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value"));
    assertThat(transformation.getDataSource().toString()).isEqualTo("{\"nice\":\"value\"}");
  }

  @Test
  void shouldExtractNullValueWhenFieldIsNotPresent() {
    final TransformRecord transformation =
        recordTransform.transform(
            JACKSON_RUNTIME,
            new MockProcessorContext(),
            KaHPPRecord.build(
                NullNode.getInstance(), json("{\"nice\":\"value\"}"), 1584352842123L));

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value"));
    assertThat(transformation.getDataSource().toString()).isEqualTo("null");
  }

  @Test
  void getNameShouldReturnConfiguredName() {
    assertThat(recordTransform.getName()).isEqualTo(STEP_NAME);
  }

  private static JsonNode json(String singleQuotedJson) {
    AtomicReference<JsonNode> json = new AtomicReference<>();

    assertDoesNotThrow(
        () ->
            json.set(
                InstanceTestConfiguration.MAPPER.readTree(singleQuotedJson.replaceAll("'", "\""))));

    return json.get();
  }
}
