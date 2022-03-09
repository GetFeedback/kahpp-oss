package dev.vox.platform.kahpp.configuration.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.junit.jupiter.api.Test;

class KafkaHeaderConverterTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final KafkaHeaderConverter kafkaHeaderConverter =
      new KafkaHeaderConverter(objectMapper);

  @Test
  void shouldCovertFromHeaders() throws JsonProcessingException {
    RecordHeaders emptyHeaders = new RecordHeaders();
    JsonNode emptyHeadersJson = kafkaHeaderConverter.convert(emptyHeaders);
    assertThat(emptyHeadersJson).isInstanceOf(ArrayNode.class);
    assertThat(emptyHeadersJson.toString()).isEqualTo("[]");

    RecordHeaders simpleHeaders = new RecordHeaders();
    simpleHeaders.add("foo", objectMapper.writeValueAsBytes("bar"));
    JsonNode simpleHeadersJson = kafkaHeaderConverter.convert(simpleHeaders);
    assertThat(simpleHeadersJson).isInstanceOf(ArrayNode.class);
    assertThat(simpleHeadersJson.toString()).isEqualTo("[{\"foo\":\"bar\"}]");

    RecordHeaders complexHeaders = new RecordHeaders();
    complexHeaders.add("foo", objectMapper.writeValueAsBytes("bar"));
    complexHeaders.add(
        "complex",
        objectMapper.writeValueAsBytes(objectMapper.createObjectNode().put("much", "complexity")));

    JsonNode complexHeadersJson = kafkaHeaderConverter.convert(complexHeaders);
    assertThat(complexHeadersJson).isInstanceOf(ArrayNode.class);
    assertThat(complexHeadersJson.toString())
        .isEqualTo("[{\"foo\":\"bar\"},{\"complex\":{\"much\":\"complexity\"}}]");

    RecordHeaders veryComplexHeaders = new RecordHeaders();
    veryComplexHeaders.add("foo", objectMapper.writeValueAsBytes("bar"));
    veryComplexHeaders.add(
        "complex",
        objectMapper.writeValueAsBytes(objectMapper.createObjectNode().put("much", "complexity")));
    veryComplexHeaders.add(
        "tooMuchComplex",
        objectMapper.writeValueAsBytes(
            objectMapper
                .createArrayNode()
                .add(objectMapper.createObjectNode().put("much", "complexity"))));

    JsonNode veryComplexHeadersJson = kafkaHeaderConverter.convert(veryComplexHeaders);
    assertThat(veryComplexHeadersJson).isInstanceOf(ArrayNode.class);
    assertThat(veryComplexHeadersJson.toString())
        .isEqualTo(
            "[{\"foo\":\"bar\"},{\"complex\":{\"much\":\"complexity\"}},{\"tooMuchComplex\":[{\"much\":\"complexity\"}]}]");
  }

  @Test
  void shouldCovertFromJson() throws JsonProcessingException {
    JsonNode emptyJsonHeaders = objectMapper.createArrayNode();
    RecordHeaders emptyRecordHeaders = kafkaHeaderConverter.convert(emptyJsonHeaders);
    assertThat(emptyRecordHeaders).isInstanceOf(RecordHeaders.class).isEqualTo(new RecordHeaders());

    JsonNode simpleJsonHeaders =
        objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("foo", "bar"));
    RecordHeaders simpleRecordHeaders = kafkaHeaderConverter.convert(simpleJsonHeaders);
    assertThat(simpleRecordHeaders)
        .isInstanceOf(RecordHeaders.class)
        .isEqualTo(new RecordHeaders().add("foo", objectMapper.writeValueAsBytes("bar")));

    JsonNode complexJsonHeaders =
        objectMapper
            .createArrayNode()
            .add(
                objectMapper
                    .createObjectNode()
                    .put("foo", "bar")
                    .put("much", "{\"much\":\"complexity\"}"));
    RecordHeaders complexRecordHeaders = kafkaHeaderConverter.convert(complexJsonHeaders);
    assertThat(complexRecordHeaders)
        .isInstanceOf(RecordHeaders.class)
        .isEqualTo(
            new RecordHeaders()
                .add("foo", objectMapper.writeValueAsBytes("bar"))
                .add("much", objectMapper.writeValueAsBytes("{\"much\":\"complexity\"}")));
  }

  @Test
  void shouldNotFailConvertingWrongFormatHeaders() {
    RecordHeaders notGoodHeaders = new RecordHeaders();
    notGoodHeaders.add("foo", "bar".getBytes(StandardCharsets.UTF_8));
    JsonNode emptyArray = kafkaHeaderConverter.convert(notGoodHeaders);
    assertThat(emptyArray).isInstanceOf(ArrayNode.class);
    assertThat(emptyArray.toString()).isEqualTo("[]");

    RecordHeaders notGoodHeadersUUID = new RecordHeaders();
    UUID uuid = UUID.randomUUID();
    notGoodHeadersUUID.add("foo", new UUIDSerializer().serialize("topic", uuid));
    JsonNode convertedJson = kafkaHeaderConverter.convert(notGoodHeadersUUID);
    assertThat(convertedJson).isInstanceOf(ArrayNode.class);
    assertThat(convertedJson.toString()).isEqualTo("[]");

    assertThat(notGoodHeadersUUID).isNotEqualTo(kafkaHeaderConverter.convert(convertedJson));
  }
}
