package dev.vox.platform.kahpp.configuration.filter;

import dev.vox.platform.kahpp.configuration.predicate.PredicateBranch;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public final class FilterTombstone implements PredicateBranch {

  @NotBlank private final transient String name;

  @Pattern(regexp = "true|false")
  private transient String filterNot = "false";

  public FilterTombstone(String name, Map<String, ?> config) {
    this.name = name;
    Object filterNot = config.get("filterNot");
    if (filterNot != null) {
      this.filterNot = filterNot.toString();
    }
  }

  @Override
  public String getJmesPath() {
    return "value";
  }

  @Override
  public boolean test(JacksonRuntime runtime, KaHPPRecord record) {
    return record.getValue() == null;
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
