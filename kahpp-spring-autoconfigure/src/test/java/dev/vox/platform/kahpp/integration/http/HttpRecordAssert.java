package dev.vox.platform.kahpp.integration.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class HttpRecordAssert
    extends AbstractAssert<HttpRecordAssert, ConsumerRecord<String, String>> {

  private HttpRecordAssert(ConsumerRecord<String, String> actual) {
    super(actual, HttpRecordAssert.class);
  }

  public static HttpRecordAssert assertThat(ConsumerRecord<String, String> actual) {
    return new HttpRecordAssert(actual);
  }

  HttpRecordAssert isEqualTo(String expectedStringfiedJson) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();

    JsonNode recordValue = objectMapper.readTree(actual.value());
    JsonNode jsonExpected = objectMapper.readTree(expectedStringfiedJson);

    String prettyJsonExpected = jsonExpected.toPrettyString();
    String prettyRecordValue = recordValue.toPrettyString();
    if (!prettyRecordValue.equals(prettyJsonExpected)) {
      failWithMessage(
          "Expected record value to be <%s> but was <%s>", prettyJsonExpected, prettyRecordValue);
    }

    return this;
  }

  HttpRecordAssert isEqualTo(Fixture expected) throws JsonProcessingException {
    return isEqualTo(expected.getValue());
  }

  HttpRecordAssert hasHeaderWithStatus(String headerStatus) {
    Header header = actual.headers().lastHeader("kahpp.tests.default.doAnAPICall");
    JsonNode headerValue = new JsonNodeDeserializer().deserialize("header", header.value());

    Assertions.assertThat(headerValue.get("status").asText()).isEqualTo(headerStatus);

    return this;
  }

  HttpRecordAssert hasNoHeaders() {
    Header header = actual.headers().lastHeader("kahpp.tests.default.doAnAPICall");
    Assertions.assertThat(header).isNull();

    return this;
  }
}
