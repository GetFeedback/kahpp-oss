package dev.vox.platform.kahpp.actuator;

import java.util.List;
import java.util.Objects;
import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamsState extends AbstractHealthIndicator {

  private final List<StreamsBuilderFactoryBean> streamsBuilders;

  public KafkaStreamsState(List<StreamsBuilderFactoryBean> streamsBuilders) {
    this.streamsBuilders = List.copyOf(streamsBuilders);
  }

  @Override
  @SuppressWarnings("PMD.CloseResource")
  protected void doHealthCheck(Health.Builder builder) {
    List<KafkaStreams> streamsList =
        streamsBuilders.stream()
            .map(StreamsBuilderFactoryBean::getKafkaStreams)
            .filter(Objects::nonNull)
            .toList();

    if (streamsList.isEmpty()) {
      builder.status(Status.UNKNOWN);
      return;
    }

    for (KafkaStreams streams : streamsList) {
      if (streams.state().hasNotStarted()) {
        builder.status(Status.UNKNOWN);
        return;
      } else if (!streams.state().isRunningOrRebalancing()) {
        builder.status(Status.DOWN);
        return;
      }
    }

    builder.status(Status.UP);
  }
}
