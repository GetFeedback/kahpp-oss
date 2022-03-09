package dev.vox.platform.kahpp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

class ApplicationTest extends AbstractKaHPPTest {

  @Autowired private transient KafkaStreamsConfiguration streamsConfig;

  @Autowired private transient StreamsBuilderFactoryBean kStreamsBuilderFactory;

  @Test
  void contextLoads() {
    Properties config = this.streamsConfig.asProperties();
    assertThat(config.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)).isTrue();
    assertThat(config.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG).toString())
        .isNotEqualTo("kafka:9092");
    assertThat(config.get(StreamsConfig.APPLICATION_ID_CONFIG).toString())
        .isEqualTo("kahpp-tests-default");
  }

  @Test
  void contextRuns() {
    assertThat(this.kStreamsBuilderFactory.isRunning()).isTrue();
  }
}
