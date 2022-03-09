package dev.vox.platform.kahpp.configuration.topic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import io.burt.jmespath.Expression;
import java.util.Objects;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;

public class Route implements Predicate<JsonNode> {
  @NotNull private final Expression<JsonNode> predicate;
  @NotNull private final TopicIdentifier topic;

  public Route(Expression<JsonNode> predicate, TopicIdentifier topic) {
    this.predicate = predicate;
    this.topic = topic;
  }

  @Override
  public boolean test(JsonNode node) {
    final JsonNode result = predicate.search(node);

    return result.isBoolean() ? result.asBoolean() : BooleanNode.getFalse().asBoolean();
  }

  public TopicIdentifier getTopic() {
    return topic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Route)) {
      return false;
    }
    Route that = (Route) o;

    return Objects.equals(predicate, that.predicate) && Objects.equals(topic, that.topic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(predicate, topic);
  }
}
