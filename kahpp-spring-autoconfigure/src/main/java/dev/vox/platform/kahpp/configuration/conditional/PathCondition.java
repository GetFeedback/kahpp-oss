package dev.vox.platform.kahpp.configuration.conditional;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import javax.validation.constraints.NotBlank;

public class PathCondition implements Condition {

  private final transient Expression<JsonNode> expression;
  @NotBlank private final transient String condition;

  public PathCondition(String condition, Expression<JsonNode> expression) {
    this.condition = condition;
    this.expression = expression;
  }

  @Override
  public boolean test(KaHPPRecord record) {
    return expression.search(record.build()).asBoolean();
  }

  @Override
  public String toString() {
    return condition;
  }
}
