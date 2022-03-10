package dev.vox.platform.kahpp.streams.serialization;

import static org.apache.kafka.common.serialization.Serdes.serdeFrom;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.streams.Instance;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class Serdes {

  private transient Map<String, Object> properties = new HashMap<>();

  /**
   * Configures KaHPP Serdes with defaults, they can be overridden in the configuration via:
   * "kahpp.streamsConfig.properties" The default configuration consists of reducing the amount of
   * magic and guesses regarding the Json serialization as KaHPP works only with JsonNode types
   * within the Steps.
   */
  public Serdes(Instance configuration) {
    properties.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

    configuration.getConfig().getProperties().forEach(properties::put);

    properties = Collections.unmodifiableMap(properties);
  }

  @Bean(name = "SerdeJsonNodeKey")
  public Serde<JsonNode> getJsonNodeKeySerde() {
    Serde<JsonNode> keySerde = serdeFrom(new JsonNodeSerializer(), new JsonNodeDeserializer());
    keySerde.configure(properties, true);
    return keySerde;
  }

  @Bean(name = "SerdeJsonNodeValue")
  public Serde<JsonNode> getJsonNodeValueSerde() {
    Serde<JsonNode> valueSerde = serdeFrom(new JsonNodeSerializer(), new JsonNodeDeserializer());
    valueSerde.configure(properties, false);
    return valueSerde;
  }

  public Map<String, Object> getProperties() {
    return Collections.unmodifiableMap(properties);
  }
}
