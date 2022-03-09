package dev.vox.platform.kahpp.test.instance.test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.test.TestRecord;

/**
 * Convenience Class which provides extra functionality to {@link TestRecord}. A KaHPPTestRecord
 * will always be of type {@code `TestRecord<JsonNode, JsonNode>`} And KaHPP Headers are of type
 * `<String, JsonNode>`, while Kafka itself only sees `byte[]`, this class then replaces the
 * serialized field in order to have better tests assertions and comparisons.
 *
 * <p>todo: Support headers from our Fixture deserialization todo: Decide if we should split the
 * Headers in two types, the ones with JsonNode value and String
 */
public class KaHPPTestRecord extends TestRecord<JsonNode, JsonNode> {

  @JsonCreator
  public KaHPPTestRecord(
      @JsonProperty("key") JsonNode key,
      @JsonProperty("value") JsonNode value,
      @JsonProperty("recordTime") Instant recordTime,
      @JsonProperty("headers") Headers headers) {
    super(key, value, headers, recordTime);
  }

  public KaHPPTestRecord(JsonNode key, JsonNode value, Headers headers, Instant recordTime) {
    super(key, value, headers, recordTime);
  }

  public static KaHPPTestRecord from(TestRecord<JsonNode, JsonNode> testRecord) {
    return new KaHPPTestRecord(
        testRecord.key(), testRecord.value(), testRecord.headers(), testRecord.getRecordTime());
  }

  @JsonProperty("headers")
  public Map<String, JsonNode> getKaHPPHeaders() throws IOException {
    Map<String, JsonNode> a = new HashMap<>();
    for (Header header : super.getHeaders()) {
      a.put(header.key(), InstanceTestConfiguration.MAPPER.readTree(header.value()));
    }

    return a;
  }
}
