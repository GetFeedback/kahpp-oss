package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.transform.UnwrapValueTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnwrapValueTransformTest {

  private static final String CONFIG_FIELD = "field";
  private static final String STEP_NAME = "name";
  private static final String WRAPPER_FIELD = "wrapper";

  private transient KaHPPRecord record;

  @BeforeEach
  void setUp() {
    final ObjectNode wrappedValue = MAPPER.createObjectNode().put("key", "value");
    final JsonNode value = MAPPER.createObjectNode().set("wrapper", wrappedValue);

    record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);
  }

  @Test
  void shouldUnwrapNestedJson() {
    final UnwrapValueTransform unwrapValueTransform =
        new UnwrapValueTransform(STEP_NAME, Map.of(CONFIG_FIELD, WRAPPER_FIELD));

    final TransformRecord transformation =
        unwrapValueTransform.transform(new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value"));
    assertThat(transformation.getDataSource().toString()).isEqualTo("{\"key\":\"value\"}");
  }

  @Test
  void shouldNotCrashWhenWrapperIsNotPresent() {
    final UnwrapValueTransform unwrapValueTransform =
        new UnwrapValueTransform(STEP_NAME, Map.of(CONFIG_FIELD, "nonExistentField"));

    final TransformRecord transformation =
        unwrapValueTransform.transform(new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(1);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("@", "value"));
    assertThat(transformation.getDataSource().toString()).isEqualTo("null");
  }

  @Test
  void getNameShouldReturnConfiguredName() {
    final UnwrapValueTransform unwrapValueTransform =
        new UnwrapValueTransform(STEP_NAME, Map.of(CONFIG_FIELD, "nonExistentField"));

    assertThat(unwrapValueTransform.getName()).isEqualTo(STEP_NAME);
  }
}
