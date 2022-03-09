package dev.vox.platform.kahpp.configuration.predicate;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.topic.ProduceToStaticRoute;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public final class PredicateOrProduceError implements PredicateBranch, ProduceToStaticRoute {

  private static final String RIGHT = "right";
  private static final String LEFT = "left";
  @NotBlank private final transient String name;
  @NotBlank private transient String jmesPath;
  @NotNull private transient TopicIdentifier topic;

  @Pattern(regexp = LEFT + "|" + RIGHT)
  private transient String either = RIGHT;

  public PredicateOrProduceError(String name, Map<String, ?> config) {
    this.name = name;
    if (config.containsKey("jmesPath")) {
      this.jmesPath = config.get("jmesPath").toString();
    }
    if (config.containsKey("either")) {
      this.either = config.get("either").toString();
    }
    if (config.containsKey(ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC)) {
      this.topic = (TopicIdentifier) config.get(ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC);
    }
  }

  @Override
  public String getJmesPath() {
    return jmesPath;
  }

  @Override
  public boolean test(JacksonRuntime runtime, KaHPPRecord record) {
    Expression<JsonNode> jsonNodeExpression = runtime.compile(jmesPath);

    return jsonNodeExpression.search(record.build()).asBoolean();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isRight() {
    return RIGHT.equals(either);
  }

  @Override
  public Set<TopicIdentifier> eligibleSinkTopics() {
    return Set.of(this.topic);
  }

  @Override
  public TopicIdentifier produceToSink(KaHPPRecord record) {
    return this.topic;
  }
}
