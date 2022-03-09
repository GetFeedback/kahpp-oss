package dev.vox.platform.kahpp.configuration.topic;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.stereotype.Component;

@Component
public class ProduceToTopicByRouteToKStream extends StepProcessorSupplier<ProduceToTopicByRoute> {

  private final transient MeterRegistry meterRegistry;

  public ProduceToTopicByRouteToKStream(MeterRegistry meterRegistry) {
    super(ProduceToTopicByRoute.class);

    this.meterRegistry = meterRegistry;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(
      ProduceToTopicByRoute step, ChildStep child) {
    return () -> new ProduceStepProcessor(step, child, meterRegistry);
  }
}
