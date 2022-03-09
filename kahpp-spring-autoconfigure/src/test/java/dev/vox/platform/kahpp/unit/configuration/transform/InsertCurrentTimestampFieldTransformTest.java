package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.transform.InsertCurrentTimestampFieldTransform;
import dev.vox.platform.kahpp.configuration.transform.RecordTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.List;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InsertCurrentTimestampFieldTransformTest {

  private static final String CONFIG_FIELD = "field";
  private static final String NEW_KEY = "newKey";
  private static final String STEP_NAME = "name";

  private transient KaHPPRecord record;

  @BeforeEach
  void setUp() {
    final JsonNode value = MAPPER.createObjectNode().put("key", "value");

    record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);
  }

  @Test
  void shouldInsertCurrentTimestampField() {
    final RecordTransform transformStep =
        new InsertCurrentTimestampFieldTransform(STEP_NAME, Map.of(CONFIG_FIELD, NEW_KEY));

    final TransformRecord transformation =
        transformStep.transform(new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getDataSource()).isInstanceOf(LongNode.class);

    final List<TransformRecord.Mutation> mutations = transformation.getMutations();
    assertThat(mutations.size()).isEqualTo(1);

    final TransformRecord.Mutation mutation = mutations.get(0);
    assertThat(mutation).isEqualTo(TransformRecord.JmesPathMutation.pair("@", "value.newKey"));
  }

  @Test
  void shouldNotInsertCurrentTimestampFieldWhenFieldHasValue() {
    final RecordTransform transformStep =
        new InsertCurrentTimestampFieldTransform(STEP_NAME, Map.of(CONFIG_FIELD, "key"));

    final TransformRecord transformation =
        transformStep.transform(new JacksonRuntime(), new MockProcessorContext(), record);

    assertThat(transformation.getMutations().size()).isEqualTo(0);
  }

  @Test
  void getNameShouldReturnConfiguredName() {
    final RecordTransform transformStep =
        new InsertCurrentTimestampFieldTransform(STEP_NAME, Map.of(CONFIG_FIELD, NEW_KEY));

    assertThat(transformStep.getName()).isEqualTo(STEP_NAME);
  }
}
