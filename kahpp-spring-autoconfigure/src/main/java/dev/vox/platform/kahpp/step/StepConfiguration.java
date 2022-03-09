package dev.vox.platform.kahpp.step;

import dev.vox.platform.kahpp.configuration.Step;
import java.util.Collections;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class StepConfiguration<T extends Step> {

  @NotNull private final transient Class<T> type;

  @NotBlank private final transient String name;

  @NotNull private final transient Map<String, ?> config;

  public StepConfiguration(Class<T> type, String name, Map<String, ?> config) {
    this.type = type;
    this.name = name;
    this.config = config != null ? Collections.unmodifiableMap(config) : Collections.emptyMap();
  }

  public StepConfiguration<T> newConfig(Map<String, ?> config) {
    return new StepConfiguration<>(this.getStepType(), this.getName(), config);
  }

  public String getName() {
    return name;
  }

  public Map<String, ?> getConfig() {
    return Collections.unmodifiableMap(config);
  }

  public Class<T> getStepType() {
    return type;
  }
}
