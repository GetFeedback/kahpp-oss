package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.micrometer.core.instrument.MeterRegistry;

public class ProduceStepProcessor extends StepProcessor<Produce> {
  public ProduceStepProcessor(Produce currentStep, ChildStep child, MeterRegistry meterRegistry) {
    super(currentStep, child, meterRegistry);
  }

  @Override
  public void process(KaHPPRecord record) {
    forwardToSink(record, step());
    forwardToNextStep(record);
  }
}
