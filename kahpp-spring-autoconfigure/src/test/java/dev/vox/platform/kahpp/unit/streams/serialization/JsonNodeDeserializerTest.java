package dev.vox.platform.kahpp.unit.streams.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeDeserializer;
import java.nio.charset.Charset;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidDuplicateLiterals"})
class JsonNodeDeserializerTest {

  private static final String KAHPP_JSON_DESERIALIZE_STRING_AS_TEXT_NODE =
      "kahpp.json.deserializer.string.as.text_node";

  @Test
  void assureConfigureMethodOnlyThrowsExceptionForNonStringOrBooleanPropertyValue() {
    JsonNodeDeserializer jsonNodeDeserializer = new JsonNodeDeserializer();

    assertThatCode(
            () -> {
              jsonNodeDeserializer.configure(
                  Map.of(KAHPP_JSON_DESERIALIZE_STRING_AS_TEXT_NODE, true), false);
              jsonNodeDeserializer.configure(
                  Map.of(KAHPP_JSON_DESERIALIZE_STRING_AS_TEXT_NODE, "true"), false);
            })
        .doesNotThrowAnyException();

    assertThatThrownBy(
            () ->
                jsonNodeDeserializer.configure(
                    Map.of(KAHPP_JSON_DESERIALIZE_STRING_AS_TEXT_NODE, 1), false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void defaultBehavior() {
    JsonNodeDeserializer jsonNodeDeserializer = new JsonNodeDeserializer();

    byte[] bytes = "random test".getBytes(Charset.defaultCharset());

    assertThatThrownBy(() -> jsonNodeDeserializer.deserialize("foo", bytes))
        .isInstanceOf(SerializationException.class);

    assertThatThrownBy(() -> jsonNodeDeserializer.deserialize("foo", new RecordHeaders(), bytes))
        .isInstanceOf(SerializationException.class);
  }

  @Test
  void stringShouldDeserializeAsTextNodeWhenOptionIsEnabled() {
    JsonNodeDeserializer jsonNodeDeserializer = new JsonNodeDeserializer();

    byte[] bytes = "random test".getBytes(Charset.defaultCharset());

    jsonNodeDeserializer.configure(Map.of(KAHPP_JSON_DESERIALIZE_STRING_AS_TEXT_NODE, true), true);

    JsonNode deserialized = jsonNodeDeserializer.deserialize("foo", bytes);

    assertThat(deserialized).isInstanceOf(TextNode.class);
    assertThat(deserialized).isEqualTo(TextNode.valueOf("random test"));

    JsonNode deserializedWithHeaders =
        jsonNodeDeserializer.deserialize("foo", new RecordHeaders(), bytes);

    assertThat(deserializedWithHeaders).isInstanceOf(TextNode.class);
    assertThat(deserializedWithHeaders).isEqualTo(TextNode.valueOf("random test"));
  }

  @Test
  void jsonStringShouldNotBeAffectedByStringToTextNodeOption() {
    JsonNodeDeserializer jsonNodeDeserializer = new JsonNodeDeserializer();

    byte[] bytes = "{\"key\": \"random text\"}".getBytes(Charset.defaultCharset());

    jsonNodeDeserializer.configure(Map.of(KAHPP_JSON_DESERIALIZE_STRING_AS_TEXT_NODE, true), false);

    JsonNode deserialized = jsonNodeDeserializer.deserialize("foo", bytes);

    assertThat(deserialized).isInstanceOf(ObjectNode.class);
    JsonNode key = deserialized.get("key");
    assertThat(key).isInstanceOf(TextNode.class);
    assertThat(key.asText()).isEqualTo("random text");

    JsonNode deserializedWithHeaders = jsonNodeDeserializer.deserialize("foo", bytes);

    assertThat(deserializedWithHeaders).isInstanceOf(ObjectNode.class);
    JsonNode keyTwo = deserialized.get("key");
    assertThat(keyTwo).isInstanceOf(TextNode.class);
    assertThat(keyTwo.asText()).isEqualTo("random text");
  }
}
