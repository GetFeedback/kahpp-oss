package dev.vox.platform.kahpp.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.vox.platform.kahpp.KafkaStreams;
import dev.vox.platform.kahpp.KafkaStreams.TopologyBuilder;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import dev.vox.platform.kahpp.streams.StepBuilderConfiguration.StepBuilderMap;
import dev.vox.platform.kahpp.streams.serialization.Serdes;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Streams;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

class KafkaStreamsTest {

  private transient KafkaStreams simpleStreams;

  @BeforeEach
  void setUp() {
    ConfigBuilder configBuilder =
        new ConfigBuilder(
            "KafkaStreamsTest",
            "canConstruct",
            1,
            Map.of("source", "KafkaStreamsTest-source"),
            new Streams(),
            Map.of(),
            List.of());
    Instance instance = new Instance(configBuilder, new StepBuilder(List.of()));
    StepBuilderMap stepBuilderMap = new StepBuilderMap();
    Serdes serdes = new Serdes(instance);

    simpleStreams =
        new KafkaStreams(
            instance, stepBuilderMap, serdes.getJsonNodeKeySerde(), serdes.getJsonNodeValueSerde());
  }

  @AfterEach
  void tearDown() {
    simpleStreams.close();
  }

  @Test
  void baseContracts() {
    assertThat(simpleStreams.getTopics().all()).contains("KafkaStreamsTest-source");
    assertThat(simpleStreams.getTopologyBuilder()).isNotNull();
    assertThat(simpleStreams.kStreamsConfigs().asProperties())
        .contains(new SimpleEntry<>("application.id", "kahpp-KafkaStreamsTest-canConstruct"));
    assertThat(simpleStreams.streamsBuilderFactoryBeanCustomizer()).isNotNull();
  }

  @Test
  @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
  void streamsBuilderFactoryBeanCustomizer() {
    StreamsBuilderFactoryBeanConfigurer expectCustomizer =
        simpleStreams.streamsBuilderFactoryBeanCustomizer();

    assertThat(expectCustomizer).isNotNull();
    StreamsBuilderFactoryBean streamsBuilderFactoryBean = new StreamsBuilderFactoryBean();
    expectCustomizer.configure(streamsBuilderFactoryBean);

    AtomicReference<TopologyBuilder> actualCustomizer = new AtomicReference<>();

    assertThatCode(
            () -> {
              Field infrastructureCustomizer =
                  StreamsBuilderFactoryBean.class.getDeclaredField("infrastructureCustomizer");
              infrastructureCustomizer.setAccessible(true);
              actualCustomizer.set(
                  (TopologyBuilder) infrastructureCustomizer.get(streamsBuilderFactoryBean));
            })
        .doesNotThrowAnyException();

    assertThat(actualCustomizer.get()).isSameAs(simpleStreams.getTopologyBuilder());
  }
}
