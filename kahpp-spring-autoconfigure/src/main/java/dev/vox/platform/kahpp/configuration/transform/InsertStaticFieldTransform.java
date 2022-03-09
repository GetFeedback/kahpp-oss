package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InsertStaticFieldTransform extends AbstractRecordTransform {

  private static final Logger LOGGER = LoggerFactory.getLogger(InsertStaticFieldTransform.class);

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new Jdk8Module());

  @NotBlank private final transient String field;
  @NotBlank private transient JsonNode value;
  @NotBlank private transient Format format = Format.STRING;

  public InsertStaticFieldTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.field = String.format("value.%s", config.get("field").toString());
    this.value = getValue(config);
  }

  private JsonNode getValue(Map<String, ?> config) {
    String value = String.valueOf(config.get("value"));
    if (config.containsKey("format")) {
      format = Format.valueOf(String.valueOf(config.get("format")).toUpperCase());
      if (Format.JSON.equals(format)) {
        try {
          return OBJECT_MAPPER.readTree(value);
        } catch (JsonProcessingException e) {
          LOGGER.error("Failed to parse value {} to JSON", value, e);
          throw new IllegalArgumentException(
              String.format("Failed to parse %s value to JSON", value));
        }
      }
    }
    return TextNode.valueOf(value);
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final Expression<JsonNode> jmesPathExpression = jacksonRuntime.compile(field);

    // If the field exists, this step won't change it's value
    if (jmesPathExpression.search(record.build()) != null
        && !jmesPathExpression.search(record.build()).isNull()) {
      LOGGER.debug("{}: field `{}` is not empty", getTypedName(), field);
      return TransformRecord.noTransformation();
    }

    return TransformRecord.replacePath(value, field);
  }

  private enum Format {
    STRING,
    JSON
  }
}
