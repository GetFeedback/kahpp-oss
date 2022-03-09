package dev.vox.platform.kahpp.step;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.streams.Instance;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class StepBuilder {

  private final transient List<ConfigurationToStep> configurationToStepList;

  public StepBuilder(List<ConfigurationToStep> configurationToStepList) {
    this.configurationToStepList = List.copyOf(configurationToStepList);
  }

  @SuppressWarnings({"unchecked"})
  public <S extends Step> S build(
      StepConfiguration<S> stepConfiguration, Instance.ConfigBuilder configBuilder) {
    Stream<ConfigurationToStep> stream = configurationToStepList.stream();

    StepConfiguration<S> updatedConfig =
        stream
            .filter(
                sConfigurationToStep -> {
                  return sConfigurationToStep
                      .supportsType()
                      .isAssignableFrom(stepConfiguration.getStepType());
                })
            .reduce(
                stepConfiguration,
                (sc, toStep) -> {
                  StepConfiguration configure = toStep.configure(sc, configBuilder);
                  return configure;
                },
                (sc, sc2) -> {
                  // This is not thread safe yet.
                  // Needs refactor: https://stackoverflow.com/a/24316429/3947202
                  throw new UnsupportedOperationException();
                });

    return this.instantiateStep(updatedConfig);
  }

  private <T extends Step> T instantiateStep(StepConfiguration<T> stepConfiguration) {
    Class<T> stepType = stepConfiguration.getStepType();
    try {
      return stepType
          .getDeclaredConstructor(String.class, Map.class)
          .newInstance(stepConfiguration.getName(), stepConfiguration.getConfig());
    } catch (InstantiationException
        | RuntimeException
        | IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException e) {
      throw new RuntimeException(
          String.format("Could not instantiate Step: `%s`", stepType.toString()), e);
    }
  }
}
