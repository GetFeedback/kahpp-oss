package dev.vox.platform.kahpp.streams.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonDeserializer;

public class JsonNodeDeserializer extends JsonDeserializer<JsonNode> {
  public static final String JSON_DESERIALIZE_STRING_AS_TEXT_NODE =
      "kahpp.json.deserializer.string.as.text_node";

  private transient boolean deserializeStringAsTextNode;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    super.configure(configs, isKey);

    if (configs.containsKey(JSON_DESERIALIZE_STRING_AS_TEXT_NODE)) {
      Object config = configs.get(JSON_DESERIALIZE_STRING_AS_TEXT_NODE);
      if (config instanceof Boolean) {
        this.deserializeStringAsTextNode = (Boolean) config;
      } else if (config instanceof String) {
        this.deserializeStringAsTextNode = Boolean.parseBoolean((String) config);
      } else {
        throw new IllegalStateException(
            JSON_DESERIALIZE_STRING_AS_TEXT_NODE + " must be Boolean or String");
      }
    }
  }

  @Override
  public JsonNode deserialize(String topic, Headers headers, byte[] data) {
    if (!this.deserializeStringAsTextNode) {
      return super.deserialize(topic, headers, data);
    }

    try {
      return super.deserialize(topic, headers, data);
    } catch (SerializationException e) {
      String string = new String(data, Charset.defaultCharset());
      return TextNode.valueOf(string);
    }
  }

  @Override
  public JsonNode deserialize(String topic, byte[] data) {
    if (!this.deserializeStringAsTextNode) {
      return super.deserialize(topic, data);
    }

    try {
      return super.deserialize(topic, data);
    } catch (SerializationException e) {
      String string = new String(data, Charset.defaultCharset());
      return TextNode.valueOf(string);
    }
  }
}
