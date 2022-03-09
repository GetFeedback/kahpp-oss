package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;

public final class KeyTransform extends AbstractRecordTransform {

  @NotBlank private final transient String jmesPath;
  private transient String wrapInField;

  public KeyTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.jmesPath = config.get("jmesPath").toString();
    if (config.containsKey("wrapInField")) {
      this.wrapInField = config.get("wrapInField").toString();
    }
  }

  // Create/replace the transform interface, the idea is to only return TransformRecord
  @Override
  public TransformRecord transform(
      JacksonRuntime runtime, ProcessorContext context, KaHPPRecord record) {
    Expression<JsonNode> jsonNodeExpression = runtime.compile(jmesPath);

    JsonNode newKey = jsonNodeExpression.search(record.build());

    if (wrapInField != null) {
      newKey = new ObjectMapper().createObjectNode().set(wrapInField, newKey);
    }

    return TransformRecord.replacePath(newKey, "key");
  }
}
