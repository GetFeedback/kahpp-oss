package dev.vox.platform.kahpp.actuator;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsProducerStateTest {

  private MeterRegistry meterRegistry;

  @BeforeEach
  public void setUp() {
    meterRegistry = new SimpleMeterRegistry();
  }

  @Test
  void statusDownWhenProducerIsNotActive() {
    meterRegistry
        .more()
        .counter(KafkaStreamsProducerState.KAFKA_PRODUCER_CONNECTION_METRIC_LABEL, Tags.empty(), 0);
    Health.Builder builder = new Health.Builder();
    new KafkaStreamsProducerState(meterRegistry).doHealthCheck(builder);
    assertThat(builder.build().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void statusUpWhenProducerIsActive() {
    meterRegistry
        .more()
        .counter(KafkaStreamsProducerState.KAFKA_PRODUCER_CONNECTION_METRIC_LABEL, Tags.empty(), 1);
    Health.Builder builder = new Health.Builder();
    new KafkaStreamsProducerState(meterRegistry).doHealthCheck(builder);
    assertThat(builder.build().getStatus()).isEqualTo(Status.UP);
  }
}
