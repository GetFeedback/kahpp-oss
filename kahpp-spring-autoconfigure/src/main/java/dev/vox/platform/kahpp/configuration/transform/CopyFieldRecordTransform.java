package dev.vox.platform.kahpp.configuration.transform;

import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import org.apache.kafka.streams.processor.ProcessorContext;

public class CopyFieldRecordTransform extends AbstractRecordTransform {

  private final transient String from;
  private final transient String to;

  public CopyFieldRecordTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.from = config.get("from").toString();
    this.to = config.get("to").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    return TransformRecord.replacePath(record.build(), from, to);
  }
}
