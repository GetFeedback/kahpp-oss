package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.configuration.transform.SplitValueException;
import dev.vox.platform.kahpp.configuration.transform.SplitValueTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SplitValueTransformTest {

  private transient KaHPPRecord record;
  private final TextNode recordKey = TextNode.valueOf("original-id");
  private final ObjectNode firstExpectedRecordValue =
      MAPPER.createObjectNode().put("id", "1").put("foo", "bar");
  private final ObjectNode secondExpectedRecordValue =
      MAPPER.createObjectNode().put("id", "2").put("foo", "baz");
  private final ObjectNode recordValueWithEmptyArrayNode =
      MAPPER.createObjectNode().put("id", "3").set("empty", MAPPER.createArrayNode());

  @BeforeEach
  void setUp() {
    final JsonNode value =
        MAPPER.createArrayNode().add(firstExpectedRecordValue).add(secondExpectedRecordValue);

    record = KaHPPRecord.build(recordKey, value, 1584352842123L);
  }

  @Test
  void shouldDemultiplexARecordIntoMultipleOnes() {
    KaHPPRecord firstExpectedRecord =
        KaHPPRecord.build(recordKey, firstExpectedRecordValue, 1584352842123L);
    KaHPPRecord secondExpectedRecord =
        KaHPPRecord.build(recordKey, secondExpectedRecordValue, 1584352842123L);

    SplitValueTransform transformStep =
        new SplitValueTransform("splitThis", Map.of("jmesPath", "value"));

    JacksonRuntime runtime = new JacksonRuntime();

    Iterator<KaHPPRecord> records = transformStep.transform(runtime, record).iterator();
    assertThat(records.hasNext()).isTrue();
    assertThat(records.next()).isEqualTo(firstExpectedRecord);
    assertThat(records.hasNext()).isTrue();
    assertThat(records.next()).isEqualTo(secondExpectedRecord);
    assertThat(records.hasNext()).isFalse();
  }

  @Test
  void shouldThrowExceptionOnNonArrayNodes() {
    SplitValueTransform transformStep =
        new SplitValueTransform("splitThis", Map.of("jmesPath", "value.foo"));

    JacksonRuntime runtime = new JacksonRuntime();

    KaHPPRecord record = KaHPPRecord.build(recordKey, firstExpectedRecordValue, 1584352842123L);

    assertThatThrownBy(() -> transformStep.transform(runtime, record))
        .as("no records should be produced to 'error' topic ")
        .isInstanceOf(SplitValueException.class)
        .hasMessage(
            "Could not split record value: data found at JmesPath value.foo is not an array");
  }

  @Test
  void shouldThrowExceptionOnEmptyArrays() {
    SplitValueTransform transformStep =
        new SplitValueTransform("splitThis", Map.of("jmesPath", "value.empty"));

    JacksonRuntime runtime = new JacksonRuntime();

    KaHPPRecord emptyRecord =
        KaHPPRecord.build(recordKey, recordValueWithEmptyArrayNode, 1584352842123L);

    assertThatThrownBy(() -> transformStep.transform(runtime, emptyRecord))
        .as("no records should be produced to 'error' topic ")
        .isInstanceOf(SplitValueException.class)
        .hasMessage(
            "Could not split record value: array found at JmesPath value.empty has no elements");
  }
}
