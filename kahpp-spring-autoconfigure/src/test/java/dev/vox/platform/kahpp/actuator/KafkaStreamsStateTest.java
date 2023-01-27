package dev.vox.platform.kahpp.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsStateTest {

  @Mock private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

  @Mock private KafkaStreams kafkaStreams;

  Map<KafkaStreams.State, Status> kafkaStateMapping =
      Map.of(
          KafkaStreams.State.CREATED, Status.UNKNOWN,
          KafkaStreams.State.RUNNING, Status.UP,
          KafkaStreams.State.REBALANCING, Status.UP,
          KafkaStreams.State.ERROR, Status.DOWN,
          KafkaStreams.State.PENDING_ERROR, Status.DOWN,
          KafkaStreams.State.PENDING_SHUTDOWN, Status.DOWN,
          KafkaStreams.State.NOT_RUNNING, Status.DOWN);

  @BeforeEach
  public void setUp() {
    lenient().when(streamsBuilderFactoryBean.getKafkaStreams()).thenReturn(kafkaStreams);
  }

  @Test
  void checkIfStatusIsUnknownWhenStreamsIsNull() {
    Health.Builder builder = new Health.Builder();
    new KafkaStreamsState(List.of()).doHealthCheck(builder);
    assertThat(builder.build().getStatus()).isEqualTo(Status.UNKNOWN);
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void checkAllKafkaStreamsStatuses() {
    KafkaStreamsState kafkaStreamsState = new KafkaStreamsState(List.of(streamsBuilderFactoryBean));
    Health.Builder builder = new Health.Builder();

    new KafkaStreamsState(List.of(streamsBuilderFactoryBean));
    Arrays.stream(KafkaStreams.State.values())
        .forEach(
            state -> {
              when(kafkaStreams.state()).thenReturn(state);
              kafkaStreamsState.doHealthCheck(builder);
              assertThat(builder.build().getStatus())
                  .as("Testing %s Kafka Stream State", state)
                  .isEqualTo(kafkaStateMapping.get(state));
            });
  }
}
