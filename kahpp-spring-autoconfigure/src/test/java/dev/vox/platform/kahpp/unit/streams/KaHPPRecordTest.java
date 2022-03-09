package dev.vox.platform.kahpp.unit.streams;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

class KaHPPRecordTest {
  private static final TextNode recordKey = TextNode.valueOf("original-id");
  private static final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");
  private static final KaHPPRecord testRecord =
      KaHPPRecord.build(recordKey, recordValue, 1584352842123L);

  @ParameterizedTest
  @MethodSource("provideValuesThatAreEqual")
  void shouldEqualTestRecord(Object comparisonValue) {
    assertThat(testRecord).isEqualTo(comparisonValue);
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideValuesThatAreEqual() {
    return Stream.of(
        testRecord,
        KaHPPRecord.build(recordKey, recordValue, 1584352842123L),
        KaHPPRecord.build(
            TextNode.valueOf("original-id"),
            MAPPER.createObjectNode().put("foo", "bar"),
            1584352842123L));
  }

  @ParameterizedTest
  @MethodSource("provideValuesThatAreNotEqual")
  @NullSource
  void shouldNotEqualTestRecord(Object comparisonValue) {
    assertThat(testRecord).isNotEqualTo(comparisonValue);
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideValuesThatAreNotEqual() {
    return Stream.of(
        recordKey,
        KaHPPRecord.build(TextNode.valueOf("new-id"), recordValue, 1584352842123L),
        KaHPPRecord.build(recordKey, MAPPER.createObjectNode().put("foo", "baz"), 1584352842123L));
  }

  @Test
  void shouldApplyRecordChangesForNewPaths() {
    final KaHPPRecord record =
        KaHPPRecord.build(NullNode.getInstance(), recordValue.deepCopy(), 1584352842123L);

    final KaHPPRecord newRecord =
        record.applyJmesPathExpression(
            new JacksonRuntime().compile("@"), "value.new.path", recordValue);

    assertThat(newRecord.getValue().get("new").get("path")).isEqualTo(recordValue);
  }

  @Test
  void shouldBeAbleToMergeEvenOnNullNodes() {
    ObjectNode thisIsNull = MAPPER.createObjectNode().set("thisIsNull", null);
    final KaHPPRecord record =
        KaHPPRecord.build(NullNode.getInstance(), thisIsNull, 1584352842123L);

    final KaHPPRecord newRecord =
        record.applyJmesPathExpression(
            new JacksonRuntime().compile("@"), "value.thisIsNull.bar", recordValue);

    assertThat(newRecord.getValue()).isEqualTo(thisIsNull.set("bar", recordValue));
  }
}
