package dev.vox.platform.kahpp.configuration.conditional;

import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance;
import java.util.HashMap;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(400)
public class ConfigurationToStepConditional implements ConfigurationToStep<Conditional> {

  private final transient ConditionFactory factory;

  public ConfigurationToStepConditional(final ConditionFactory factory) {
    this.factory = factory;
  }

  @Override
  public StepConfiguration<Conditional> configure(
      StepConfiguration<Conditional> stepConfiguration, Instance.ConfigBuilder configBuilder) {
    HashMap<String, Object> config = new HashMap<>(stepConfiguration.getConfig());

    Condition condition = Condition.ALWAYS;
    if (config.containsKey(Conditional.STEP_CONFIGURATION_CONDITION)) {
      condition =
          factory.createCondition(config.get(Conditional.STEP_CONFIGURATION_CONDITION).toString());
    }

    config.put(Conditional.STEP_CONFIGURATION_CONDITION, condition);
    return stepConfiguration.newConfig(config);
  }

  @Override
  public Class<Conditional> supportsType() {
    return Conditional.class;
  }
}
