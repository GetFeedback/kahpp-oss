package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConvertZonedDateTimeFieldTransform extends AbstractRecordTransform {

  public static final String FIELD_INPUT_FORMAT = "inputFormat";
  public static final String FIELD_OUTPUT_FORMAT = "outputFormat";

  public static final String INTERNAL_FIELD_INPUT_FORMAT = "inputFormatter";
  public static final String INTERNAL_FIELD_OUTPUT_FORMAT = "outputFormatter";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ConvertZonedDateTimeFieldTransform.class);

  @NotNull private final transient DateTimeFormatter inputFormatter;
  @NotNull private final transient DateTimeFormatter outputFormatter;

  @Pattern(regexp = "(key|value).*(\\.)\\w+")
  private final transient String field;

  public ConvertZonedDateTimeFieldTransform(@NotBlank String name, Map<String, ?> config) {
    super(name, config);
    this.field = config.get("field").toString();
    this.inputFormatter = (DateTimeFormatter) config.get("inputFormatter");
    this.outputFormatter = (DateTimeFormatter) config.get("outputFormatter");
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final Expression<JsonNode> jmesPathExpression = jacksonRuntime.compile(field);

    JsonNode field = jmesPathExpression.search(record.build());
    if (!field.isTextual()) {
      LOGGER.warn(
          "{}: field `{}` is not a text/string value, skipping DateTime conversion",
          getTypedName(),
          this.field);
      return TransformRecord.noTransformation();
    }

    try {
      var value = ZonedDateTime.parse(field.asText(), inputFormatter);
      return TransformRecord.replacePath(value.format(outputFormatter), this.field);
    } catch (DateTimeParseException e) {
      LOGGER.warn(
          "{}: field `{}` with value `{}` is not a valid DateTime",
          getTypedName(),
          this.field,
          field.asText());
      return TransformRecord.noTransformation();
    }
  }
}
