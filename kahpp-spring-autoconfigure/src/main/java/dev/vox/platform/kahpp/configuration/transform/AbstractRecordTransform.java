package dev.vox.platform.kahpp.configuration.transform;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.conditional.Condition;
import java.util.Map;

public abstract class AbstractRecordTransform implements RecordTransform, Step {

  private final transient String name;

  private transient Condition condition = Condition.ALWAYS;

  protected AbstractRecordTransform(String name, Map<String, ?> config) {
    this.name = name;
    if (config.containsKey(STEP_CONFIGURATION_CONDITION)) {
      this.condition = (Condition) config.get(STEP_CONFIGURATION_CONDITION);
    }
  }

  @Override
  public Condition condition() {
    return this.condition;
  }

  @Override
  public String getName() {
    return name;
  }
}
