package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.TransformRecordApplier;
import dev.vox.platform.kahpp.configuration.transform.MoveFieldRecordTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.Test;

class MoveFieldRecordTransformTest {

  private final JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private final MockProcessorContext mockProcessorContext = new MockProcessorContext();

  @Test
  void shouldTransform() {
    final JsonNode value = MAPPER.createObjectNode().put("foo", "bar");
    KaHPPRecord record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);

    final MoveFieldRecordTransform moveFieldRecordTransform =
        new MoveFieldRecordTransform(
            "moveFieldRecordTransformTest", Map.of("from", "value.foo", "to", "value.moveHere"));

    TransformRecord transform =
        moveFieldRecordTransform.transform(jacksonRuntime, mockProcessorContext, record);
    TransformRecordApplier.apply(jacksonRuntime, record, transform);

    assertThat(record.build().toString())
        .isEqualTo(
            "{\"key\":null,\"value\":{\"moveHere\":\"bar\"},\"timestamp\":1584352842123,\"headers\":null}");
  }

  @Test
  void shouldKeepIdempotency() {
    final JsonNode value = MAPPER.createObjectNode().put("foo", "bar");
    KaHPPRecord record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);

    final MoveFieldRecordTransform moveFieldRecordTransform =
        new MoveFieldRecordTransform(
            "moveFieldRecordTransformTest", Map.of("from", "value.foo", "to", "value.moveHere"));

    TransformRecord transform =
        moveFieldRecordTransform.transform(jacksonRuntime, mockProcessorContext, record);
    TransformRecordApplier.apply(jacksonRuntime, record, transform);
    TransformRecord transform2 =
        moveFieldRecordTransform.transform(jacksonRuntime, mockProcessorContext, record);
    TransformRecordApplier.apply(jacksonRuntime, record, transform2);

    assertThat(record.build().toString())
        .isEqualTo(
            "{\"key\":null,\"value\":{\"moveHere\":\"bar\"},\"timestamp\":1584352842123,\"headers\":null}");
  }
}
