package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class FlatRecordTransformStepToKStream
    extends StepProcessorSupplier<FlatRecordTransform> {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(FlatRecordTransformStepToKStream.class);

  private final transient MeterRegistry meterRegistry;
  private final transient JacksonRuntime jacksonRuntime;

  @Autowired
  public FlatRecordTransformStepToKStream(
      MeterRegistry meterRegistry, JacksonRuntime jacksonRuntime) {
    super(FlatRecordTransform.class);
    this.meterRegistry = meterRegistry;
    this.jacksonRuntime = jacksonRuntime;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(FlatRecordTransform step, ChildStep child) {
    return () -> new FlatRecordTransformStepProcessor(step, child);
  }

  public class FlatRecordTransformStepProcessor extends StepProcessor<FlatRecordTransform> {

    private final transient FlatRecordTransform step;

    public FlatRecordTransformStepProcessor(FlatRecordTransform step, ChildStep child) {
      super(step, child, meterRegistry);
      this.step = step;
    }

    @Override
    public void process(KaHPPRecord record) {
      List<KaHPPRecord> records = step.transform(jacksonRuntime, record);

      LOGGER.debug(
          "{}: Flat record transformation {}",
          step.getTypedName(),
          records.isEmpty() ? "skipped" : "applied");

      if (!records.isEmpty()) {
        LOGGER.debug(
            "{}: {} records created from original record", step.getTypedName(), records.size());
      }

      records.iterator().forEachRemaining(this::forwardToNextStep);
    }
  }
}
