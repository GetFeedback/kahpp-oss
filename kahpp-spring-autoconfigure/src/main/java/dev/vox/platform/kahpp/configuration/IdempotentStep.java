package dev.vox.platform.kahpp.configuration;

import dev.vox.platform.kahpp.streams.InstanceRuntime;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * IdempotentStep assure idempotency to a step. Each idempotent step is checked on @see
 * dev.vox.platform.kahpp.processor.StepProcessor, if the header of the record processed contains a
 * `success header` the step will be skipped, otherwise at the end, before forward it, the record
 * will be set as done.
 */
public interface IdempotentStep extends Step {

  default Boolean isAlreadyDone(ProcessorContext context) {
    return InstanceRuntime.HeaderHelper.isSuccessStepHeader(this, context.headers());
  }

  default void setAsDone(ProcessorContext context) {
    context.headers().add(InstanceRuntime.HeaderHelper.forSuccess(this));
  }
}
