package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * This class pulls a field out of a complex value and replaces the entire value with the extracted
 * field.
 */
public final class ExtractFieldValueTransform extends AbstractRecordTransform {

  @NotBlank private final transient String field;

  public ExtractFieldValueTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.field = config.get("field").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final JsonNode value = record.getValue();

    final Expression<JsonNode> expression = jacksonRuntime.compile(field);
    final JsonNode extractedValue = expression.search(value);

    return TransformRecord.replacePath(extractedValue, "value");
  }
}
