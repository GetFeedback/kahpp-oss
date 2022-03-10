package dev.vox.platform.kahpp.streams.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class JsonNodeSerializer extends JsonSerializer<JsonNode> {

  public static final String JSON_SERIALIZE_TEXT_NODE_AS_STRING =
      "kahpp.json.serializer.text_node.as.string";
  public static final String JSON_SERIALIZE_NULL_NODE_AS_NULL =
      "kahpp.json.serializer.null_node.as.null";

  private transient boolean serializeTextNodeAsString;
  private transient boolean serializeNullNodeAsNull;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    super.configure(configs, isKey);

    if (configs.containsKey(JSON_SERIALIZE_TEXT_NODE_AS_STRING)) {
      Object config = configs.get(JSON_SERIALIZE_TEXT_NODE_AS_STRING);
      if (config instanceof Boolean) {
        this.serializeTextNodeAsString = (Boolean) config;
      } else if (config instanceof String) {
        this.serializeTextNodeAsString = Boolean.parseBoolean((String) config);
      } else {
        throw new IllegalStateException(
            JSON_SERIALIZE_TEXT_NODE_AS_STRING + " must be Boolean or String");
      }
    }

    if (configs.containsKey(JSON_SERIALIZE_NULL_NODE_AS_NULL)) {
      Object config = configs.get(JSON_SERIALIZE_NULL_NODE_AS_NULL);
      if (config instanceof Boolean) {
        this.serializeNullNodeAsNull = (Boolean) config;
      } else if (config instanceof String) {
        this.serializeNullNodeAsNull = Boolean.parseBoolean((String) config);
      } else {
        throw new IllegalStateException(
            JSON_SERIALIZE_NULL_NODE_AS_NULL + " must be Boolean or String");
      }
    }
  }

  @Override
  public byte[] serialize(String topic, JsonNode data) {
    if (serializeTextNodeAsString && data instanceof TextNode) {
      return data.asText().getBytes(StandardCharsets.UTF_8);
    }

    if (serializeNullNodeAsNull && data instanceof NullNode) {
      return null;
    }

    return super.serialize(topic, data);
  }
}
