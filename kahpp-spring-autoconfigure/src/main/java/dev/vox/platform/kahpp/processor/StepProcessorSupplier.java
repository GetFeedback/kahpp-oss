package dev.vox.platform.kahpp.processor;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.step.ChildStep;
import org.apache.kafka.streams.processor.ProcessorSupplier;

public abstract class StepProcessorSupplier<T extends Step> {

  private final transient Class<T> clazz;

  protected StepProcessorSupplier(Class<T> clazz) {
    this.clazz = clazz;
  }

  public abstract ProcessorSupplier<JsonNode, JsonNode> supplier(T step, ChildStep child);

  public Class<? extends Step> getType() {
    return clazz;
  }
}
