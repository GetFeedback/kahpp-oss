package dev.vox.platform.kahpp.configuration.conditional;

import dev.vox.platform.kahpp.configuration.Step;
import javax.validation.constraints.NotNull;

/**
 * This step behaviour can skip a step based on a condition. Skipping a step means that the Record
 * will be forwarded to the next step without being executed. This behaviour is enabled by
 * `StepProcessor` and the skipping process occur there. Here is very important to understand that
 * this "configuration" does not work as `PredicateBranch`/`Filter` because these steps can
 * `TERMINATE` a Record, and here we `FORWARD` the record without executing the step.
 */
public interface Conditional extends Step {
  String STEP_CONFIGURATION_CONDITION = "condition";

  @NotNull
  Condition condition();
}
