package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;

public final class SplitValueTransform implements FlatRecordTransform {

  @NotBlank private final transient String name;
  @NotBlank private final transient String jmesPath;

  public SplitValueTransform(String name, Map<String, ?> config) {
    this.name = name;
    this.jmesPath = config.get("jmesPath").toString();
  }

  @Override
  public List<KaHPPRecord> transform(JacksonRuntime runtime, KaHPPRecord record) {
    Expression<JsonNode> jsonNodeExpression = runtime.compile(jmesPath);

    JsonNode possibleArrayNode = jsonNodeExpression.search(record.build());
    if (!(possibleArrayNode instanceof ArrayNode)) {
      throw new SplitValueException(
          String.format(
              "Could not split record value: data found at JmesPath %s is not an array", jmesPath));
    }

    if (possibleArrayNode.isEmpty()) {
      throw new SplitValueException(
          String.format(
              "Could not split record value: array found at JmesPath %s has no elements",
              jmesPath));
    }

    List<KaHPPRecord> records = new ArrayList<>();

    possibleArrayNode
        .iterator()
        .forEachRemaining(
            jsonNode ->
                records.add(
                    KaHPPRecord.build(
                        record.getKey(), jsonNode, record.getTimestamp(), record.getHeaders())));

    return records;
  }

  @Override
  public String getName() {
    return name;
  }
}
