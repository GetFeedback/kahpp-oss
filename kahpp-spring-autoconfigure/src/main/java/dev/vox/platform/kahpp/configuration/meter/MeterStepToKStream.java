package dev.vox.platform.kahpp.configuration.meter;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class MeterStepToKStream extends StepProcessorSupplier<Meter> {
  private final transient MeterRegistry meterRegistry;

  private final transient JacksonRuntime jacksonRuntime;

  @Autowired
  public MeterStepToKStream(MeterRegistry meterRegistry, JacksonRuntime jacksonRuntime) {
    super(Meter.class);
    this.meterRegistry = meterRegistry;
    this.jacksonRuntime = jacksonRuntime;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(Meter step, ChildStep child) {
    return () -> new MeterStepProcessor(step, child);
  }

  private class MeterStepProcessor extends StepProcessor<Meter> {

    private final transient Meter meterStep;

    public MeterStepProcessor(Meter meterStep, ChildStep child) {
      super(meterStep, child, meterRegistry);
      this.meterStep = meterStep;
    }

    @Override
    public void process(KaHPPRecord record) {
      meterStep.use(meterRegistry, jacksonRuntime, record.getKey(), record.getValue());

      forwardToNextStep(record);
    }
  }
}
