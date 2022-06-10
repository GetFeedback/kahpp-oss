package dev.vox.platform.kahpp.step;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.streams.Instance;
import java.util.HashMap;
import java.util.Map;

public interface ConfigurationToStep<T extends Step> {
  StepConfiguration<T> configure(
      StepConfiguration<T> stepConfiguration, Instance.ConfigBuilder configBuilder);

  Class<T> supportsType();

  default Map<String, Object> getConfigAsHashMap(StepConfiguration<T> tStepConfiguration) {
    return new HashMap<>(tStepConfiguration.getConfig());
  }
}
