package dev.vox.platform.kahpp.configuration.transform;

import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.conditional.Conditional;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import org.apache.kafka.streams.processor.ProcessorContext;

public interface RecordTransform extends Conditional {

  TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record);
}
