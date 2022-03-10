package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.List;
import java.util.Map;
import org.apache.kafka.streams.processor.ProcessorContext;

public class MoveFieldRecordTransform extends AbstractRecordTransform {

  private final transient String from;
  private final transient String to;

  public MoveFieldRecordTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.from = config.get("from").toString();
    this.to = config.get("to").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    ObjectNode object = (ObjectNode) record.build();
    if (!jacksonRuntime.compile(from).search(object).isNull()) {
      return TransformRecord.withMutations(
          object,
          List.of(
              TransformRecord.JmesPathMutation.pair(from, to),
              TransformRecord.RemoveFieldMutation.field(from)));
    }
    return TransformRecord.noTransformation();
  }
}
