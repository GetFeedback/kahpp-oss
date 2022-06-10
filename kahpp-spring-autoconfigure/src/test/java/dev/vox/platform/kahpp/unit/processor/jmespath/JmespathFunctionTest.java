package dev.vox.platform.kahpp.unit.processor.jmespath;

import com.fasterxml.jackson.databind.JsonNode;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;
import org.junit.jupiter.api.BeforeEach;

abstract class JmespathFunctionTest {
  private JacksonRuntime runtime;

  @BeforeEach
  public void setUp() {
    RuntimeConfiguration configuration =
        RuntimeConfiguration.builder().withFunctionRegistry(functionRegistry()).build();

    runtime = new JacksonRuntime(configuration);
  }

  protected abstract FunctionRegistry functionRegistry();

  protected JsonNode evaluate(String expression) {
    return runtime.compile(expression).search(parse("{}"));
  }

  protected JsonNode parse(String string) {
    return runtime.parseString(string);
  }

  protected JsonNode parse(Long l) {
    return runtime.createNumber(l);
  }
}
