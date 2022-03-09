package dev.vox.platform.kahpp.unit.configuration.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.conditional.ConditionFactory;
import io.burt.jmespath.jackson.JacksonRuntime;
import org.junit.jupiter.api.Test;

class ConditionFactoryTest {
  @Test
  void shouldReturnACondition() {
    final ConditionFactory conditionFactory = new ConditionFactory(new JacksonRuntime());

    assertThat(conditionFactory.createCondition("value==`true`")).isInstanceOf(Condition.class);
  }
}
