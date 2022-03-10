package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * This class extracts a header to an item value. If there is already the entry but nullable on the
 * value, this transformation will override the value
 */
public final class HeaderToValueTransform extends AbstractRecordTransform {

  @NotBlank private final transient String header;
  @NotBlank private final transient String field;

  public HeaderToValueTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.header = config.get("header").toString();
    this.field = config.get("field").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final JsonNode value = record.getValue();
    if (value.has(field) && !value.get(field).isNull()) {
      return TransformRecord.noTransformation();
    }

    Iterator<Header> headerIterator = context.headers().headers(header).iterator();
    if (headerIterator.hasNext()) {
      return TransformRecord.replacePath(
          new String(headerIterator.next().value(), StandardCharsets.UTF_8),
          String.format("value.%s", field));
    }

    return TransformRecord.noTransformation();
  }
}
