package dev.vox.platform.kahpp.configuration.meter;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.step.StepMetricUtils;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public final class CounterMeter implements Meter {

  public static final String JMESPATH_METRIC_NON_MATCH_DEFAULT_VALUE = "unknown";

  @NotBlank private final transient String name;
  /**
   * We have a limitation now on how we manage the jmx exporter configuration, by having more tags
   * the exporter regex match would have to change, a possible future solution is to allow the user
   * to amend the jmx configuration with their own regex.
   */
  @Size(min = 1, max = 1)
  private final transient Map<String, String> tags;

  @SuppressWarnings("unchecked")
  public CounterMeter(String name, Map<String, ?> config) {
    this.name = name;
    this.tags = (Map<String, String>) config.get("tags");
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Creates a metric using JMESPath to count how many items with the same value (based on the JMES
   * path) were processed. This metric is a counter using increment.
   */
  @Override
  public void use(
      MeterRegistry meterRegistry, JacksonRuntime runtime, JsonNode key, JsonNode value) {
    Map<String, Expression<JsonNode>> compiledTags = new HashMap<>();

    tags.forEach((tag, jmesPathString) -> compiledTags.put(tag, runtime.compile(jmesPathString)));

    meterRegistry
        .counter(StepMetricUtils.formatMetricName(getName()), getTags(compiledTags, value))
        .increment();
  }

  private List<Tag> getTags(Map<String, Expression<JsonNode>> compiledTags, JsonNode jsonNode) {
    List<Tag> builtTags = new ArrayList<>(StepMetricUtils.getStepTags(this));

    compiledTags.forEach(
        (tagKey, jsonNodeExpression) ->
            builtTags.add(
                Tag.of(
                    tagKey,
                    jsonNodeExpression
                        .search(jsonNode)
                        .asText(JMESPATH_METRIC_NON_MATCH_DEFAULT_VALUE))));

    return Collections.unmodifiableList(builtTags);
  }
}
