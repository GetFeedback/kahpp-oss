package dev.vox.platform.kahpp.configuration.throttle;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ThrottleStepToKStream extends StepProcessorSupplier<Throttle> {
  private final transient MeterRegistry meterRegistry;

  @Autowired
  public ThrottleStepToKStream(MeterRegistry meterRegistry) {
    super(Throttle.class);
    this.meterRegistry = meterRegistry;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(Throttle step, ChildStep child) {
    return () -> new ThrottleStepProcessor(step, child);
  }

  private class ThrottleStepProcessor extends StepProcessor<Throttle> {

    private final transient RateLimiter rateLimiter;

    public ThrottleStepProcessor(Throttle step, ChildStep child) {
      super(step, child, meterRegistry);
      this.rateLimiter = new GuavaRateLimiter(step().getRecordsPerSecond());
    }

    @Override
    public void process(KaHPPRecord record) {
      rateLimiter.acquire();

      forwardToNextStep(record);
    }
  }
}
