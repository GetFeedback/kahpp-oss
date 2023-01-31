package dev.vox.platform.kahpp.actuator;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.util.Optional;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

@Component
public class KafkaStreamsProducerState extends AbstractHealthIndicator {

  private final MeterRegistry meterRegistry;

  public static final String KAFKA_PRODUCER_CONNECTION_METRIC_LABEL =
      "kafka.producer.connection.count";

  public KafkaStreamsProducerState(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    Search connectionsSearch = meterRegistry.find(KAFKA_PRODUCER_CONNECTION_METRIC_LABEL);
    Double kafkaConnections =
        Optional.ofNullable(connectionsSearch.functionCounter())
            .map(FunctionCounter::count)
            .orElse(0d);
    if (kafkaConnections > 0) {
      builder.status(Status.UP);
      return;
    }
    builder.status(Status.DOWN);
  }
}
