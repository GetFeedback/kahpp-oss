package dev.vox.platform.kahpp.unit.streams.serialization;

import static dev.vox.platform.kahpp.streams.serialization.JsonNodeSerializer.JSON_SERIALIZE_NULL_NODE_AS_NULL;
import static dev.vox.platform.kahpp.streams.serialization.JsonNodeSerializer.JSON_SERIALIZE_TEXT_NODE_AS_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeSerializer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.CloseResource")
class JsonNodeSerializerTest {

  @Test
  void assureConfigureMethodOnlyThrowsExceptionForNonStringOrBooleanPropertyValue() {
    JsonNodeSerializer jsonNodeSerializer = new JsonNodeSerializer();

    assertThatCode(
            () -> {
              jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_TEXT_NODE_AS_STRING, true), false);
              jsonNodeSerializer.configure(
                  Map.of(JSON_SERIALIZE_TEXT_NODE_AS_STRING, "true"), false);
              jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_NULL_NODE_AS_NULL, true), false);
              jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_NULL_NODE_AS_NULL, "true"), false);
            })
        .doesNotThrowAnyException();

    assertThatThrownBy(
            () ->
                jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_TEXT_NODE_AS_STRING, 1), false))
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(
            () -> jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_NULL_NODE_AS_NULL, 1), false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void textNodeWithConfigOnShouldReturnANonQuotedString() {
    JsonNodeSerializer jsonNodeSerializer = new JsonNodeSerializer();

    TextNode textNode = new TextNode("xablau");

    jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_TEXT_NODE_AS_STRING, true), false);

    byte[] serialized = jsonNodeSerializer.serialize("", textNode);

    assertThat(serialized).isEqualTo("xablau".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void textNodeWithConfigOffShouldReturnAQuotedString() {
    JsonNodeSerializer jsonNodeSerializer = new JsonNodeSerializer();

    TextNode textNode = new TextNode("xablau");

    byte[] serialized = jsonNodeSerializer.serialize("", textNode);

    assertThat(serialized).isEqualTo("\"xablau\"".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void nonTextNodeValueShouldHaveTheSameBehaviorWithTextNodeConfigOnAndOff() {
    JsonNodeSerializer serializerWithConversion = new JsonNodeSerializer();
    serializerWithConversion.configure(Map.of(JSON_SERIALIZE_TEXT_NODE_AS_STRING, true), false);

    JsonNodeSerializer serializerDefault = new JsonNodeSerializer();
    ObjectMapper mapper = new ObjectMapper();

    List<JsonNode> jsonNodeList =
        List.of(
            new BigIntegerNode(new BigInteger("12")),
            NullNode.getInstance(),
            mapper.createObjectNode().put("foo", true).get("foo"),
            mapper.createArrayNode(),
            mapper.createObjectNode(),
            mapper.createObjectNode().put("foo", "bar"));

    for (JsonNode jsonNode : jsonNodeList) {
      assertThat(serializerDefault.serialize("", jsonNode))
          .isEqualTo(serializerWithConversion.serialize("", jsonNode));
    }
  }

  @Test
  void nullNodeWithConfigOnShouldReturnNull() {
    JsonNodeSerializer jsonNodeSerializer = new JsonNodeSerializer();

    jsonNodeSerializer.configure(Map.of(JSON_SERIALIZE_NULL_NODE_AS_NULL, true), false);

    byte[] serialized = jsonNodeSerializer.serialize("", NullNode.getInstance());

    assertThat(serialized).isNull();
  }

  @Test
  void nullNodeWithConfigOffShouldReturnAQuotedString() {
    JsonNodeSerializer jsonNodeSerializer = new JsonNodeSerializer();

    byte[] serialized = jsonNodeSerializer.serialize("", NullNode.getInstance());

    assertThat(serialized).isEqualTo("null".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void nonNullNodeValueShouldHaveTheSameBehaviorWithNullNodeConfigOnAndOff() {
    JsonNodeSerializer serializerWithConversion = new JsonNodeSerializer();
    serializerWithConversion.configure(Map.of(JSON_SERIALIZE_NULL_NODE_AS_NULL, true), false);

    JsonNodeSerializer serializerDefault = new JsonNodeSerializer();
    ObjectMapper mapper = new ObjectMapper();

    List<JsonNode> jsonNodeList =
        List.of(
            new BigIntegerNode(new BigInteger("12")),
            new TextNode("xablau"),
            mapper.createObjectNode().put("foo", true).get("foo"),
            mapper.createArrayNode(),
            mapper.createObjectNode(),
            mapper.createObjectNode().put("foo", "bar"));

    for (JsonNode jsonNode : jsonNodeList) {
      assertThat(serializerDefault.serialize("", jsonNode))
          .isEqualTo(serializerWithConversion.serialize("", jsonNode));
    }
  }
}
