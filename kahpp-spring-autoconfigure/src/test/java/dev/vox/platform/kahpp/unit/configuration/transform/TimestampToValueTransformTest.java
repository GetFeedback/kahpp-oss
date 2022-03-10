package dev.vox.platform.kahpp.unit.configuration.transform;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.transform.TimestampToValueTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import java.util.TimeZone;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimestampToValueTransformTest {

  private static final ObjectMapper mapper = new ObjectMapper();
  public static final String VALUE_FIELD_DATETIME = "myTime";
  private transient MockProcessorContext mockProcessorContext;
  private transient ObjectNode key;
  private transient ObjectNode value;

  @BeforeEach
  void setUp() {
    mockProcessorContext = new MockProcessorContext();
    key = mapper.createObjectNode();
    value = mapper.createObjectNode();

    // This same instruction happens on
    // dev.vox.platform.kahpp.KafkaStreams.KafkaStreams
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  @Test
  void checkDefaultTimeZone() {
    assertThat(TimeZone.getDefault()).isEqualTo(TimeZone.getTimeZone("UTC"));
  }

  @Test
  void withDefaultDateFormat() {
    mockProcessorContext.setTimestamp(1581764553232L);
    TimestampToValueTransform transformer =
        new TimestampToValueTransform("noConfig", Map.of("field", VALUE_FIELD_DATETIME));

    TransformRecord transform =
        transformer.transform(
            new JacksonRuntime(),
            mockProcessorContext,
            KaHPPRecord.build(key, value, 1584352842123L));

    assertThat(transform.getMutations().size()).isEqualTo(1);
    TransformRecord.JmesPathMutation mutation =
        (TransformRecord.JmesPathMutation) transform.getMutations().get(0);
    assertThat(mutation.getJmesTo()).isEqualTo(String.format("value.%s", VALUE_FIELD_DATETIME));

    JsonNode transformedValue = transform.getDataSource();
    assertThat(transformedValue).isNotNull();
    assertThat(transformedValue.asText()).isEqualTo("2020-02-15T11:02:33+0000");
  }

  @Test
  void withCustomDateFormat() {
    mockProcessorContext.setTimestamp(1581764554242L);
    TimestampToValueTransform transformer =
        new TimestampToValueTransform(
            "noConfig",
            Map.of("field", VALUE_FIELD_DATETIME, "format", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

    TransformRecord transform =
        transformer.transform(
            new JacksonRuntime(),
            mockProcessorContext,
            KaHPPRecord.build(key, value, 1584352842123L));

    JsonNode transformedValue = transform.getDataSource();
    assertThat(transformedValue).isNotNull();
    assertThat(transformedValue.asText()).isEqualTo("2020-02-15T11:02:34.242+0000");
  }

  @Test
  void timezoneCanNotInfluenceFormat() {
    System.setProperty("user.timezone", "PST");
    // You'd expect to see <"2020-02-15T03:02:34.242-0800">

    mockProcessorContext.setTimestamp(1581764554242L);
    TimestampToValueTransform transformer =
        new TimestampToValueTransform(
            "noConfig",
            Map.of("field", VALUE_FIELD_DATETIME, "format", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

    TransformRecord transform =
        transformer.transform(
            new JacksonRuntime(),
            mockProcessorContext,
            KaHPPRecord.build(key, value, 1584352842123L));

    JsonNode transformedValue = transform.getDataSource();
    assertThat(transformedValue).isNotNull();
    assertThat(transformedValue.asText()).isEqualTo("2020-02-15T11:02:34.242+0000");
  }
}
