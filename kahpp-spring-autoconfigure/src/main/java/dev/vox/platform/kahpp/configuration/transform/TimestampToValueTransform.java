package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;

/** Extracts the Record Timestamp and inserts in a given field in the Value. */
public final class TimestampToValueTransform extends AbstractRecordTransform {

  private static final String DATE_FORMAT_RFC3339 = "yyyy-MM-dd'T'HH:mm:ssZ";

  @NotBlank private final transient String field;
  private transient String format = DATE_FORMAT_RFC3339;

  public TimestampToValueTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.field = config.get("field").toString();

    if (config.containsKey("format")) {
      this.format = config.get("format").toString();
    }
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final JsonNode value = record.getValue();
    // Possibly allow to overwrite the value based on a configuration
    if (value.has(field) && !value.get(field).isNull()) {
      return TransformRecord.noTransformation();
    }

    return TransformRecord.replacePath(
        generateFieldFromTimestamp(context), String.format("value.%s", field));
  }

  private String generateFieldFromTimestamp(ProcessorContext context) {
    return DateTimeFormatter.ofPattern(format)
        .withLocale(Locale.ROOT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(context.timestamp()));
  }
}
