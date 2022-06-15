package dev.vox.platform.kahpp.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.vox.platform.kahpp.configuration.util.KafkaHeaderConverter;
import io.burt.jmespath.Expression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class KaHPPRecord {
  private static final int PIECES_WITH_ONLY_ONE_ITEM = 1;
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new Jdk8Module());
  private static final KafkaHeaderConverter KAFKA_HEADER_CONVERTER =
      new KafkaHeaderConverter(OBJECT_MAPPER);
  private final transient JsonNode key;
  private final transient JsonNode value;
  private final transient long timestamp;
  private transient JsonNode headers;

  private KaHPPRecord(JsonNode key, JsonNode value, long timestamp, JsonNode headers) {
    this.key = key;
    this.value = value;
    this.timestamp = timestamp;
    this.headers = headers;
  }

  private KaHPPRecord(JsonNode key, JsonNode value, long timestamp) {
    this.key = key;
    this.value = value;
    this.timestamp = timestamp;
  }

  public static KaHPPRecord build(JsonNode key, JsonNode value, long timestamp, Headers headers) {
    return new KaHPPRecord(key, value, timestamp, KAFKA_HEADER_CONVERTER.convert(headers));
  }

  public static KaHPPRecord build(JsonNode key, JsonNode value, long timestamp, JsonNode headers) {
    return new KaHPPRecord(key, value, timestamp, headers);
  }

  public static KaHPPRecord build(JsonNode key, JsonNode value, long timestamp) {
    return new KaHPPRecord(key, value, timestamp);
  }

  public JsonNode getKey() {
    return key;
  }

  public JsonNode getValue() {
    return value;
  }

  public JsonNode getHeaders() {
    return headers;
  }

  public RecordHeaders getRecordHeaders() {
    return KAFKA_HEADER_CONVERTER.convert(this.headers);
  }

  public JsonNode build() {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode dataSource = mapper.createObjectNode();

    dataSource.set("key", key);
    dataSource.set("value", value);
    dataSource.put("timestamp", timestamp);
    dataSource.set("headers", headers);

    return dataSource;
  }

  public KaHPPRecord applyJmesPathExpression(
      Expression<JsonNode> fromExpression, String toExpression, JsonNode dataSource) {
    JsonNode recordJsonNode = build();

    JsonNode search = fromExpression.search(dataSource);

    if (toExpression.matches("^headers.*")) {
      recordJsonNode = changeJsonNodeHeaderWithJmesPath(recordJsonNode, toExpression, search);
    } else {
      recordJsonNode = changeJsonNodeValueWithJmesPath(recordJsonNode, toExpression, search);
    }

    return KaHPPRecord.build(
        recordJsonNode.get("key"),
        recordJsonNode.get("value"),
        timestamp,
        recordJsonNode.get("headers"));
  }

  /*
   * Unfortunately we couldn't find yet a better way to do Json mutations with proper JMESPath,
   * at least not without adopting a whole different stack than Jackson.
   * todo: Find something better :(
   */
  private static JsonNode changeJsonNodeValueWithJmesPath(
      JsonNode source, String replaceJmesPath, JsonNode replacement) {

    List<String> pieces = Arrays.asList(replaceJmesPath.split("\\."));
    String current = pieces.get(0);

    if (pieces.size() == PIECES_WITH_ONLY_ONE_ITEM) {
      if (source.isObject()) {
        return ((ObjectNode) source).set(current, replacement);
      } else {
        return OBJECT_MAPPER.createObjectNode().set(current, replacement);
      }
    }

    if (!source.has(current)) {
      ((ObjectNode) source).set(current, OBJECT_MAPPER.createObjectNode());
    }

    JsonNode childSourceNode = source.get(current);
    if (childSourceNode.isNull()) {
      childSourceNode = OBJECT_MAPPER.createObjectNode();
    }

    childSourceNode =
        changeJsonNodeValueWithJmesPath(
            childSourceNode, String.join(".", pieces.subList(1, pieces.size())), replacement);

    return ((ObjectNode) source).set(current, childSourceNode);
  }

  private static JsonNode changeJsonNodeHeaderWithJmesPath(
      JsonNode source, String replaceJmesPath, JsonNode replacement) {

    ArrayNode headers = OBJECT_MAPPER.createArrayNode();
    if (source.hasNonNull("headers")) {
      headers = ((ArrayNode) source.get("headers"));
    }
    String field = replaceJmesPath.replaceFirst("headers.", "");
    headers.addObject().set(field, replacement);

    return ((ObjectNode) source).set("headers", headers);
  }

  public KaHPPRecord applyRemoveField(String field) {
    List<String> pieces = new ArrayList<>(Arrays.asList(field.split("\\.")));
    ObjectNode objectNode = removeNestedField((ObjectNode) build(), pieces);
    return KaHPPRecord.build(objectNode.get("key"), objectNode.get("value"), timestamp);
  }

  private ObjectNode removeNestedField(ObjectNode dataSource, List<String> pieces) {
    String currentField = pieces.get(0);
    if (pieces.size() == PIECES_WITH_ONLY_ONE_ITEM) {
      dataSource.remove(currentField);
      return dataSource;
    }

    JsonNode childNode = dataSource.get(currentField);
    pieces.remove(0);

    if (childNode instanceof ObjectNode) {
      dataSource.set(currentField, removeNestedField((ObjectNode) childNode, pieces));
    }

    return dataSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof KaHPPRecord)) {
      return false;
    }
    KaHPPRecord that = (KaHPPRecord) o;
    return Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getKey(), getValue());
  }

  public Long getTimestamp() {
    return timestamp;
  }
}
