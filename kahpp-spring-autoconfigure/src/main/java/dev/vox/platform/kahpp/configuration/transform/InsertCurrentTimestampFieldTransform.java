package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.time.Instant;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InsertCurrentTimestampFieldTransform extends AbstractRecordTransform {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(InsertCurrentTimestampFieldTransform.class);

  @NotBlank private final transient String field;

  public InsertCurrentTimestampFieldTransform(String name, Map<String, ?> config) {
    super(name, config);
    this.field = String.format("value.%s", config.get("field").toString());
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    final Expression<JsonNode> jmesPathExpression = jacksonRuntime.compile(field);

    // If the field exists, this step won't change it's value
    if (jmesPathExpression.search(record.build()) != null
        && !jmesPathExpression.search(record.build()).isNull()) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(getTypedName() + ": field `" + field + "` is not empty");
      }
      return TransformRecord.noTransformation();
    }

    final long currentTimestamp = Instant.now().toEpochMilli();
    final LongNode timestampNode = LongNode.valueOf(currentTimestamp);
    return TransformRecord.replacePath(timestampNode, field);
  }
}
