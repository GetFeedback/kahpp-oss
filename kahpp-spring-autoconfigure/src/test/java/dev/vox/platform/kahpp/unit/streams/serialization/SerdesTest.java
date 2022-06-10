package dev.vox.platform.kahpp.unit.streams.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import dev.vox.platform.kahpp.streams.serialization.Serdes;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SerdesTest {

  @Test
  void defaultConfigurationValue() throws NoSuchFieldException, IllegalAccessException {
    Serdes serdes = createSerdesWithProperties(Map.of());

    assertThat(serdes.getProperties())
        .contains(
            new SimpleEntry<>("spring.json.trusted.packages", "*"),
            new SimpleEntry<>("spring.json.use.type.headers", false),
            new SimpleEntry<>("spring.json.add.type.headers", false));
  }

  @Test
  void configurationIsUnmodifiable() throws NoSuchFieldException, IllegalAccessException {
    Serdes serdes = createSerdesWithProperties(Map.of());

    Map<String, Object> properties = serdes.getProperties();

    assertThatThrownBy(() -> properties.put("foo", "bar"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void addNewProperty() throws NoSuchFieldException, IllegalAccessException {
    Serdes serdes = createSerdesWithProperties(Map.of("new.property", true));

    assertThat(serdes.getProperties())
        .contains(
            new SimpleEntry<>("spring.json.trusted.packages", "*"),
            new SimpleEntry<>("spring.json.use.type.headers", false),
            new SimpleEntry<>("spring.json.add.type.headers", false),
            new SimpleEntry<>("new.property", true));
  }

  @Test
  void overrideDefaultConfigurationValue() throws NoSuchFieldException, IllegalAccessException {
    Serdes serdes = createSerdesWithProperties(Map.of("spring.json.trusted.packages", "new"));

    assertThat(serdes.getProperties())
        .contains(
            new SimpleEntry<>("spring.json.trusted.packages", "new"),
            new SimpleEntry<>("spring.json.use.type.headers", false),
            new SimpleEntry<>("spring.json.add.type.headers", false));
  }

  @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
  private static Serdes createSerdesWithProperties(Map<String, Object> properties)
      throws NoSuchFieldException, IllegalAccessException {
    KafkaProperties.Streams streamsConfig = new KafkaProperties.Streams();

    Field propertiesField = KafkaProperties.Streams.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    propertiesField.set(streamsConfig, properties);

    Instance configuration =
        new Instance(
            new ConfigBuilder(
                "tests", "default", null, Map.of(), streamsConfig, Map.of(), List.of()),
            new StepBuilder(List.of()));

    return new Serdes(configuration);
  }
}
