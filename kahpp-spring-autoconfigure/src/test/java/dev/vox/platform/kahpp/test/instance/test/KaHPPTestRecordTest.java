package dev.vox.platform.kahpp.test.instance.test;

import static dev.vox.platform.kahpp.integration.AbstractKaHPPTest.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Instant;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.Test;

class KaHPPTestRecordTest {

  @Test
  void from() {
    JsonNode key = OBJECT_MAPPER.createObjectNode().put("key", 1);
    JsonNode value = OBJECT_MAPPER.createObjectNode().put("value", "foo");
    TestRecord<JsonNode, JsonNode> testRecord = new TestRecord<JsonNode, JsonNode>(key, value);
    KaHPPTestRecord kaHPPTestRecord = KaHPPTestRecord.from(testRecord);
    assertThat(kaHPPTestRecord).isNotNull();
    assertThat(kaHPPTestRecord.getKey()).isEqualTo(key);
    assertThat(kaHPPTestRecord.getValue()).isEqualTo(value);
  }

  @Test
  void getKaHPPHeaders() throws IOException {
    JsonNode key = OBJECT_MAPPER.createObjectNode().put("key", 1);
    JsonNode value = OBJECT_MAPPER.createObjectNode().put("value", "foo");

    RecordHeaders headers = new RecordHeaders();
    headers.add(new RecordHeader("operation", OBJECT_MAPPER.writeValueAsBytes("cake")));

    KaHPPTestRecord kaHPPTestRecord =
        new KaHPPTestRecord(key, value, headers, Instant.ofEpochMilli(123456));
    assertThat(kaHPPTestRecord).isNotNull();
    assertThat(kaHPPTestRecord.getKey()).isEqualTo(key);
    assertThat(kaHPPTestRecord.getValue()).isEqualTo(value);
    assertThat(kaHPPTestRecord.getKaHPPHeaders())
        .containsEntry("operation", OBJECT_MAPPER.createObjectNode().textNode("cake"));
  }
}
