package dev.vox.platform.kahpp.configuration.topic;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.stereotype.Component;

@Component
public class ProduceToTopicToKStream extends StepProcessorSupplier<ProduceToTopic> {

  private final transient MeterRegistry meterRegistry;

  public ProduceToTopicToKStream(MeterRegistry meterRegistry) {
    super(ProduceToTopic.class);

    this.meterRegistry = meterRegistry;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(ProduceToTopic step, ChildStep child) {
    return () -> new ProduceStepProcessor(step, child, meterRegistry);
  }
}
