package dev.vox.platform.kahpp.configuration.filter;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.predicate.PredicateBranch;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public final class FilterField implements PredicateBranch {

  @NotBlank private final transient String name;
  @NotBlank private final transient String jmesPath;

  @Pattern(regexp = "true|false")
  private transient String filterNot = "false";

  public FilterField(String name, Map<String, ?> config) {
    this.name = name;
    this.jmesPath = config.get("jmesPath").toString();
    Object filterNot = config.get("filterNot");
    if (filterNot != null) {
      this.filterNot = filterNot.toString();
    }
  }

  @Override
  public String getJmesPath() {
    return jmesPath;
  }

  @Override
  public boolean test(JacksonRuntime runtime, KaHPPRecord record) {
    // todo: Move the jmesPath compilation to a Configuration Step
    Expression<JsonNode> jsonNodeExpression = runtime.compile(jmesPath);

    return jsonNodeExpression.search(record.build()).asBoolean();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isRight() {
    return !Boolean.parseBoolean(filterNot);
  }
}
