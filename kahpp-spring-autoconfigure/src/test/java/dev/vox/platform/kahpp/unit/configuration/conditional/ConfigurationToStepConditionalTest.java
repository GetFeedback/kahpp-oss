package dev.vox.platform.kahpp.unit.configuration.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.conditional.ConditionFactory;
import dev.vox.platform.kahpp.configuration.conditional.Conditional;
import dev.vox.platform.kahpp.configuration.conditional.ConfigurationToStepConditional;
import dev.vox.platform.kahpp.configuration.conditional.PathCondition;
import dev.vox.platform.kahpp.step.StepConfiguration;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationToStepConditionalTest {

  private transient ConfigurationToStepConditional configurationToStep;

  @BeforeEach
  void setUp() {
    configurationToStep =
        new ConfigurationToStepConditional(new ConditionFactory(new JacksonRuntime()));
  }

  @Test
  void shouldSupportConditionalStep() {
    assertThat(configurationToStep.supportsType()).isEqualTo(Conditional.class);
  }

  @Test
  void shouldReplaceWithAlwaysConditionWhenConditionIsEmpty() {
    final StepConfiguration<Conditional> stepConfiguration =
        new StepConfiguration<>(Conditional.class, "stepName", Map.of());
    final StepConfiguration<Conditional> configuredStepConfiguration =
        configurationToStep.configure(stepConfiguration, null);

    assertThat(configuredStepConfiguration.getConfig().get("condition"))
        .isEqualTo(Condition.ALWAYS);
  }

  @Test
  void shouldReplaceWithPathConditionWhenConditionIsPresent() {
    final StepConfiguration<Conditional> stepConfiguration =
        new StepConfiguration<>(
            Conditional.class, "stepName", Map.of("condition", "value==`true`"));
    final StepConfiguration<Conditional> configuredStepConfiguration =
        configurationToStep.configure(stepConfiguration, null);

    assertThat(configuredStepConfiguration.getConfig().get("condition"))
        .isInstanceOf(PathCondition.class);
  }
}
