package dev.vox.platform.kahpp.unit.configuration.filter;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.configuration.filter.FilterField;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import dev.vox.platform.kahpp.streams.StepBuilderConfiguration;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

class FilterFieldTest extends ConstraintViolationTestAbstract {

  private static final String NAME = "test-name";
  private static final JacksonRuntime jacksonRuntime =
      new StepBuilderConfiguration().jacksonRuntime();

  @Test
  public void canConstruct() {
    FilterField filter = new FilterField(NAME, Map.of("jmesPath", "value == 0"));
    Set<ConstraintViolation<FilterField>> violations = validator.validate(filter);

    assertThat(violations).hasSize(0);
    assertThat(filter.getName()).isEqualTo(NAME);
    assertThat(filter.getJmesPath()).isEqualTo("value == 0");
    assertThat(filter.isRight()).isTrue();
  }

  @Test
  public void canConstructWithFilterNot() {
    FilterField filter =
        new FilterField(NAME, Map.of("jmesPath", "value == 0", "filterNot", "true"));
    Set<ConstraintViolation<FilterField>> violations = validator.validate(filter);

    assertThat(violations).hasSize(0);
    assertThat(filter.getName()).isEqualTo(NAME);
    assertThat(filter.getJmesPath()).isEqualTo("value == 0");
    assertThat(filter.isRight()).isFalse();
  }

  @Test
  public void canFilterByTimestamp() {
    final TextNode recordKey = TextNode.valueOf("test-record-id");
    final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");
    final KaHPPRecord recordWithValue = KaHPPRecord.build(recordKey, recordValue, 1584352842123L);

    FilterField filter = new FilterField(NAME, Map.of("jmesPath", "timestamp == `1584352842123`"));

    boolean result = filter.test(jacksonRuntime, recordWithValue);
    assertThat(result).isTrue();
  }

  @Test
  public void canFilterByValue() {
    final TextNode recordKey = TextNode.valueOf("test-record-id");
    final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");
    final KaHPPRecord recordWithValue = KaHPPRecord.build(recordKey, recordValue, 1584352842123L);

    FilterField filter = new FilterField(NAME, Map.of("jmesPath", "value.foo == 'bar'"));

    boolean result = filter.test(jacksonRuntime, recordWithValue);
    assertThat(result).isTrue();
  }

  @Test
  public void canFilterByKey() {
    final TextNode recordKey = TextNode.valueOf("test-record-id");
    final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");
    final KaHPPRecord recordWithValue = KaHPPRecord.build(recordKey, recordValue, 1584352842123L);

    FilterField filter = new FilterField(NAME, Map.of("jmesPath", "key == 'test-record-id'"));

    boolean result = filter.test(jacksonRuntime, recordWithValue);
    assertThat(result).isTrue();
  }

  @Test
  public void canFilterByTimestampFail() {
    final TextNode recordKey = TextNode.valueOf("test-record-id");
    final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");
    final KaHPPRecord recordWithValue = KaHPPRecord.build(recordKey, recordValue, 1584352842123L);

    FilterField filter = new FilterField(NAME, Map.of("jmesPath", "timestamp == `1000000000`"));

    boolean result = filter.test(jacksonRuntime, recordWithValue);
    assertThat(result).isFalse();
  }

  @Test
  public void canFilterByTimestampUsingNowFun() {
    final TextNode recordKey = TextNode.valueOf("test-record-id");
    final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");
    final KaHPPRecord recordWithValue = KaHPPRecord.build(recordKey, recordValue, 1633341600L);

    FilterField filterWithPlusZero =
        new FilterField(NAME, Map.of("jmesPath", "timestamp < now('+P5D')"));
    assertThat(filterWithPlusZero.test(jacksonRuntime, recordWithValue)).isTrue();
  }
}
