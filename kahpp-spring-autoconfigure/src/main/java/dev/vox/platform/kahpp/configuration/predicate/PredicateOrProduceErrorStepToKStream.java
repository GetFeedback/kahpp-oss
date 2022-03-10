package dev.vox.platform.kahpp.configuration.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class PredicateOrProduceErrorStepToKStream
    extends StepProcessorSupplier<PredicateOrProduceError> {

  private final transient MeterRegistry meterRegistry;
  private final transient JacksonRuntime jacksonRuntime;

  @Autowired
  public PredicateOrProduceErrorStepToKStream(
      MeterRegistry meterRegistry, JacksonRuntime jacksonRuntime) {
    super(PredicateOrProduceError.class);
    this.meterRegistry = meterRegistry;
    this.jacksonRuntime = jacksonRuntime;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(
      PredicateOrProduceError step, ChildStep child) {
    return () -> new PredicateStepProcessor(step, child, meterRegistry, jacksonRuntime);
  }
}
