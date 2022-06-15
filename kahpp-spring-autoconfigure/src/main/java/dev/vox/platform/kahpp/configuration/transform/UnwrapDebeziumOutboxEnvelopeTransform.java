package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.IdempotentStep;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * This Transform unwraps the Debezium Outbox Envelope in order to have the payload as the root
 * element of the message.
 *
 * @see <a
 *     href="https://debezium.io/documentation/reference/1.6/transformations/outbox-event-router.html">Debezium
 *     Outbox Event Router documentation</a>
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class UnwrapDebeziumOutboxEnvelopeTransform extends AbstractRecordTransform
    implements Step, IdempotentStep {

  @NotBlank private transient String prefix = "vox.outbox.";
  @NotBlank private transient boolean copyHeaders;

  public UnwrapDebeziumOutboxEnvelopeTransform(String name, Map<String, ?> config) {
    super(name, config);

    if (config.containsKey("copyEnvelopeFieldsToHeaders")) {
      this.copyHeaders =
          Boolean.parseBoolean(String.valueOf(config.get("copyEnvelopeFieldsToHeaders")));
    }
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {

    Version version = getVersion(context.headers());
    boolean isNotVersionOne =
        !(Version.ONE.equals(version) || Version.ONE_IMPLICIT.equals(version));
    if (isNotVersionOne) {
      return TransformRecord.noTransformation();
    }

    if (!isValidOutboxPayload(record.getValue())) {
      throw new UnwrapDebeziumOutboxEnvelopeException(
          String.format(
              "Could not use payload: expected an Outbox V1 Payload but got something else, key: %s",
              record.getKey()));
    }

    JsonNode payload = record.getValue().get("payload");

    if (payload.getNodeType() != JsonNodeType.STRING) {
      throw new UnwrapDebeziumOutboxEnvelopeException(
          String.format(
              "Could not extract payload: expected a stringified JSON but got %s",
              payload.getNodeType()));
    }

    ObjectNode value = (ObjectNode) record.getValue();
    try {
      JsonNode jsonPayload = jacksonRuntime.parseString(payload.textValue());
      value.set("payload", jsonPayload);
    } catch (IllegalStateException e) {
      throw new UnwrapDebeziumOutboxEnvelopeException(
          String.format(
              "Could not parsing payload to JsonNode: got error when trying to parse %s",
              record.getKey()),
          e);
    }

    List<TransformRecord.Mutation> mutations = new ArrayList<>();
    if (copyHeaders) {
      mutations.addAll(
          List.of(
              TransformRecord.JmesPathMutation.pair(
                  "type", String.format("headers.%sevent", prefix)),
              TransformRecord.JmesPathMutation.pair(
                  "aggregateId", String.format("headers.%saggregateId", prefix)),
              TransformRecord.JmesPathMutation.pair(
                  "aggregateType", String.format("headers.%saggregateType", prefix)),
              TransformRecord.JmesPathMutation.pair(
                  "payloadType", String.format("headers.%spayloadType", prefix))));

      if (payload.has("createdAt")) {
        mutations.add(
            TransformRecord.JmesPathMutation.pair(
                "createdAt", String.format("headers.%screated", prefix)));
      }
    }
    mutations.addAll(
        List.of(
            TransformRecord.JmesPathMutation.pair("aggregateId", "key.id"),
            TransformRecord.JmesPathMutation.pair("payload", "value")));

    // todo remove this soon
    context.headers().add("vox.outbox.metaVersion", "\"1\"".getBytes(StandardCharsets.UTF_8));
    return TransformRecord.replacePaths(value, mutations);
  }

  private Version getVersion(Headers headers) {
    String headerValue = "";
    Header header = headers.lastHeader("vox.outbox.metaVersion");
    if (header != null) {
      headerValue = new String(header.value(), StandardCharsets.UTF_8);
    }
    return Version.getByValue(headerValue);
  }

  private boolean isValidOutboxPayload(JsonNode payload) {
    return payload.has("type")
        && payload.has("aggregateId")
        && payload.has("aggregateType")
        && payload.has("payloadType")
        && payload.has("payload");
  }

  private enum Version {
    /** ONE_IMPLICIT is Version ONE without explicit header `vox.outbox.metaVersion`. */
    ONE_IMPLICIT(""),
    ONE("1"),
    TWO("2");

    public final String value;
    private static final Map<String, Version> byValue = new HashMap<>();

    static {
      for (Version e : values()) {
        byValue.put(e.value, e);
      }
    }

    public static Version getByValue(String value) {
      return byValue.get(value);
    }

    Version(String value) {
      this.value = value;
    }
  }
}
