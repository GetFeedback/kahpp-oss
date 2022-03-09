package dev.vox.platform.kahpp.configuration.conditional;

import io.burt.jmespath.jackson.JacksonRuntime;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConditionFactory {

  private final transient JacksonRuntime runtime;

  public ConditionFactory(final JacksonRuntime runtime) {
    this.runtime = runtime;
  }

  public Condition createCondition(String condition) {
    return new PathCondition(condition, runtime.compile(condition));
  }
}
