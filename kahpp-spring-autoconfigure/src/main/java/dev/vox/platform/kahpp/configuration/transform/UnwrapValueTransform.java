package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;

/** This class unwraps the content of one field to root value (`@`). */
public final class UnwrapValueTransform extends AbstractRecordTransform {

  @NotBlank private final transient String field;

  public UnwrapValueTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.field = config.get("field").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final Expression<JsonNode> jmesPathExpression = jacksonRuntime.compile(field);
    final JsonNode recordValue = record.getValue();

    return TransformRecord.replacePath(jmesPathExpression.search(recordValue), "value");
  }
}
