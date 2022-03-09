package dev.vox.platform.kahpp.configuration.meter;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.Step;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;

public interface Meter extends Step {
  void use(MeterRegistry meterRegistry, JacksonRuntime runtime, JsonNode key, JsonNode value);
}
