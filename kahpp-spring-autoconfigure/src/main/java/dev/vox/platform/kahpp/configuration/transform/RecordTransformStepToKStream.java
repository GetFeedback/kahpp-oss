package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.TransformRecordApplier;
import dev.vox.platform.kahpp.processor.Start;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class RecordTransformStepToKStream
    extends StepProcessorSupplier<AbstractRecordTransform> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordTransformStepToKStream.class);

  private final transient MeterRegistry meterRegistry;
  private final transient JacksonRuntime jacksonRuntime;

  @Autowired
  public RecordTransformStepToKStream(MeterRegistry meterRegistry, JacksonRuntime jacksonRuntime) {
    super(AbstractRecordTransform.class);
    this.meterRegistry = meterRegistry;
    this.jacksonRuntime = jacksonRuntime;
  }

  @Override
  public ProcessorSupplier<JsonNode, JsonNode> supplier(
      AbstractRecordTransform step, ChildStep child) {
    return () -> new AbstractRecordTransformStepProcessor(step, child);
  }

  public class AbstractRecordTransformStepProcessor extends StepProcessor<AbstractRecordTransform> {

    private final transient AbstractRecordTransform step;

    public AbstractRecordTransformStepProcessor(AbstractRecordTransform step, ChildStep child) {
      super(step, child, meterRegistry);
      this.step = step;
    }

    @Override
    public void process(KaHPPRecord record) {
      TransformRecord transformRecord = step.transform(jacksonRuntime, context(), record);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "{}: Record transformation {}",
            step.getTypedName(),
            transformRecord.getMutations().isEmpty() ? "skipped" : "applied");
      }
      KaHPPRecord newRecord = TransformRecordApplier.apply(jacksonRuntime, record, transformRecord);
      // These two lines should go away from here soon
      newRecord.getRecordHeaders().forEach(r -> context().headers().remove(r.key()));
      newRecord.getRecordHeaders().forEach(r -> context().headers().add(r));

      if (!record.getKey().equals(newRecord.getKey())) {
        MDC.put(Start.MDC_KAFKA_MESSAGE_KEY, newRecord.getKey().toString());
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "{}: changed Record key from `{}` to `{}`",
              step.getTypedName(),
              record.getKey(),
              newRecord.getKey());
        }
      }

      forwardToNextStep(newRecord);
    }
  }
}
