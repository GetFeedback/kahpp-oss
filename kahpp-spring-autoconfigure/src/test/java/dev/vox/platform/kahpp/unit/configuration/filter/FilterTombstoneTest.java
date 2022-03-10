package dev.vox.platform.kahpp.unit.configuration.filter;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.configuration.filter.FilterTombstone;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

class FilterTombstoneTest extends ConstraintViolationTestAbstract {

  private static final String NAME = "test-name";
  private static final JacksonRuntime jacksonRuntime = new JacksonRuntime();

  private final FilterTombstone filter = new FilterTombstone(NAME, Map.of());

  private final TextNode recordKey = TextNode.valueOf("test-record-id");
  private final ObjectNode recordValue = MAPPER.createObjectNode().put("foo", "bar");

  private final KaHPPRecord tombStoneRecord = KaHPPRecord.build(recordKey, null, 1584352842123L);
  private final KaHPPRecord recordWithValue =
      KaHPPRecord.build(recordKey, recordValue, 1584352842123L);

  @Test
  public void canConstruct() {
    Set<ConstraintViolation<FilterTombstone>> violations = validator.validate(filter);

    assertThat(violations).hasSize(0);
    assertThat(filter.getName()).isEqualTo(NAME);
    assertThat(filter.getJmesPath()).isEqualTo("value");
    assertThat(filter.isRight()).isTrue();
  }

  @Test
  public void canConstructWithConfig() {
    FilterTombstone filter = new FilterTombstone(NAME, Map.of("filterNot", "true"));

    Set<ConstraintViolation<FilterTombstone>> violations = validator.validate(filter);

    assertThat(violations).hasSize(0);
    assertThat(filter.getName()).isEqualTo(NAME);
    assertThat(filter.getJmesPath()).isEqualTo("value");
    assertThat(filter.isRight()).isFalse();
  }

  @Test
  public void canFilterRecordsWithNullValues() {
    assertThat(filter.test(jacksonRuntime, tombStoneRecord)).isTrue();
    assertThat(filter.test(jacksonRuntime, recordWithValue)).isFalse();
  }
}
