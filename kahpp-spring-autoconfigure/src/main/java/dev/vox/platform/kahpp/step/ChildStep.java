package dev.vox.platform.kahpp.step;

import dev.vox.platform.kahpp.configuration.Step;
import org.apache.kafka.streams.processor.To;

public class ChildStep {
  private final transient To child;
  private final transient String name;

  public ChildStep(Step step) {
    this(step.getTypedName());
  }

  public ChildStep(String name) {
    this.name = name;
    this.child = To.child(name);
  }

  public To to() {
    return child;
  }

  public String getName() {
    return name;
  }
}
