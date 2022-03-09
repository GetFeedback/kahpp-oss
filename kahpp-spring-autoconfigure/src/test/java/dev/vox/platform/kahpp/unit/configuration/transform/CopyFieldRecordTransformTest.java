package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.TransformRecordApplier;
import dev.vox.platform.kahpp.configuration.transform.CopyFieldRecordTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.Test;

class CopyFieldRecordTransformTest {

  private final JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private final MockProcessorContext mockProcessorContext = new MockProcessorContext();

  @Test
  void shouldTransform() {
    final JsonNode value = MAPPER.createObjectNode().put("foo", "bar");
    KaHPPRecord record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);

    final CopyFieldRecordTransform copyFieldRecordTransform =
        new CopyFieldRecordTransform(
            "copyFieldRecordTransformTest", Map.of("from", "value.foo", "to", "value.copyHere"));

    TransformRecord transform =
        copyFieldRecordTransform.transform(jacksonRuntime, mockProcessorContext, record);

    KaHPPRecord transformedRecord = TransformRecordApplier.apply(jacksonRuntime, record, transform);
    assertThat(transformedRecord.build().toString())
        .isEqualTo(
            "{\"key\":null,\"value\":{\"foo\":\"bar\",\"copyHere\":\"bar\"},\"timestamp\":1584352842123,\"headers\":null}");
  }

  @Test
  void shouldKeepIdempotency() {
    final JsonNode value = MAPPER.createObjectNode().put("foo", "bar");
    KaHPPRecord record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);

    final CopyFieldRecordTransform copyFieldRecordTransform =
        new CopyFieldRecordTransform(
            "copyFieldRecordTransformTest", Map.of("from", "value.foo", "to", "value.copyHere"));

    TransformRecord transform =
        copyFieldRecordTransform.transform(jacksonRuntime, mockProcessorContext, record);
    KaHPPRecord transformedRecord = TransformRecordApplier.apply(jacksonRuntime, record, transform);
    TransformRecord transform2 =
        copyFieldRecordTransform.transform(jacksonRuntime, mockProcessorContext, record);
    KaHPPRecord transformedRecord2 =
        TransformRecordApplier.apply(jacksonRuntime, transformedRecord, transform2);

    assertThat(transformedRecord2.build().toString())
        .isEqualTo(
            "{\"key\":null,\"value\":{\"foo\":\"bar\",\"copyHere\":\"bar\"},\"timestamp\":1584352842123,\"headers\":null}");
  }
}
