package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.TransformRecordApplier;
import dev.vox.platform.kahpp.configuration.transform.InsertStaticFieldTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InsertStaticFieldTransformTest {

  private static final String CONFIG_FIELD = "field";
  private static final String CONFIG_VALUE = "value";
  private static final String NEW_VALUE = "newValue";
  private static final String NEW_KEY = "newKey";
  private static final String STEP_NAME = "name";
  private static final String FORMAT_FIELD = "format";
  private static final String CONFIG_OVERRIDE = "overrideIfExists";

  private transient KaHPPRecord record;

  @BeforeEach
  void setUp() {
    final JsonNode value = MAPPER.createObjectNode().put("key", "value");

    record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);
  }

  @Test
  void shouldInsertStaticField() {
    final InsertStaticFieldTransform insertStaticFieldTransform =
        new InsertStaticFieldTransform(
            STEP_NAME, Map.of(CONFIG_FIELD, NEW_KEY, CONFIG_VALUE, NEW_VALUE));

    final TransformRecord transformation =
        insertStaticFieldTransform.transform(
            new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value.newKey"));
    assertThat(transformation.getDataSource()).isEqualTo(TextNode.valueOf(NEW_VALUE));
  }

  @Test
  void shouldNotInsertStaticFieldWhenFieldHasValue() {
    final InsertStaticFieldTransform insertStaticFieldTransform =
        new InsertStaticFieldTransform(
            STEP_NAME, Map.of(CONFIG_FIELD, "key", CONFIG_VALUE, NEW_VALUE));

    final TransformRecord transformation =
        insertStaticFieldTransform.transform(
            new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(0);
  }

  @Test
  void getNameShouldReturnConfiguredName() {
    final InsertStaticFieldTransform insertStaticFieldTransform =
        new InsertStaticFieldTransform(
            STEP_NAME, Map.of(CONFIG_FIELD, NEW_KEY, CONFIG_VALUE, NEW_VALUE));

    assertThat(insertStaticFieldTransform.getName()).isEqualTo(STEP_NAME);
  }

  @Test
  void shouldInsertStaticFieldArray() {

    final InsertStaticFieldTransform insertStaticFieldTransform =
        new InsertStaticFieldTransform(
            STEP_NAME,
            Map.of(
                CONFIG_FIELD, NEW_KEY, CONFIG_VALUE, "[\"foo\", \"bar\"]", FORMAT_FIELD, "json"));

    final TransformRecord transformation =
        insertStaticFieldTransform.transform(
            new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value.newKey"));

    ArrayNode expected = MAPPER.createArrayNode().add("foo").add("bar");
    assertThat(transformation.getDataSource()).isEqualTo(expected);
  }

  @Test
  void shouldThrowException() {
    assertThatThrownBy(
            () ->
                new InsertStaticFieldTransform(
                    STEP_NAME,
                    Map.of(
                        CONFIG_FIELD,
                        NEW_KEY,
                        CONFIG_VALUE,
                        "foo bar in string",
                        FORMAT_FIELD,
                        "json")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to parse foo bar in string value to JSON");

    assertThatThrownBy(
            () ->
                new InsertStaticFieldTransform(
                    STEP_NAME,
                    Map.of(
                        CONFIG_FIELD,
                        NEW_KEY,
                        CONFIG_VALUE,
                        "[[\"foo\", \"bar\"]",
                        FORMAT_FIELD,
                        "json")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to parse [[\"foo\", \"bar\"] value to JSON");
  }

  @Test
  void shouldOverrideFieldIfExists() {
    KaHPPRecord record =
        KaHPPRecord.build(
            NullNode.getInstance(),
            MAPPER.createObjectNode().put("foo", "bar").put("overrideThis", "oldValue"),
            1584352842123L);

    final InsertStaticFieldTransform insertStaticFieldTransform =
        new InsertStaticFieldTransform(
            STEP_NAME,
            Map.of(CONFIG_FIELD, "overrideThis", CONFIG_VALUE, NEW_VALUE, CONFIG_OVERRIDE, true));

    JacksonRuntime runtime = new JacksonRuntime();
    final TransformRecord transformation =
        insertStaticFieldTransform.transform(runtime, new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value.overrideThis"));
    assertThat(transformation.getDataSource()).isEqualTo(TextNode.valueOf(NEW_VALUE));

    TransformRecordApplier.apply(runtime, record, transformation);
    assertThat(record.build().toString())
        .isEqualTo(
            """
        {"key":null,"value":{"foo":"bar","overrideThis":"newValue"},"timestamp":1584352842123,"headers":null}
        """
                .trim());
  }
}
