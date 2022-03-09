package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * This class wraps the current content in a single field. If the value already has the field, the
 * transformation will be skipped.
 */
public final class WrapValueTransform extends AbstractRecordTransform {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @NotBlank private final transient String field;

  public WrapValueTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.field = config.get("field").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final JsonNode value = record.getValue();
    // For now the standard behavior assumes that if the key already exists there's no
    // need to wrap it again, depending on the use case it might interesting to know
    // it in order to change the behavior, for instance fail or re-wrap the object
    if (value.has(field)) {
      return TransformRecord.noTransformation();
    }

    final ObjectNode wrappedValue = MAPPER.createObjectNode();

    wrappedValue.set(field, value);

    return TransformRecord.replacePath(wrappedValue, "value");
  }
}
