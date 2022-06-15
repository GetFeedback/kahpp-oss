package dev.vox.platform.kahpp.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.step.ChildStep;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.stream.Stream;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HandleByStatusCodeStepToKStreamTest {

  @ParameterizedTest
  @MethodSource("params")
  void testSupplier(
      SimpleMeterRegistry meterRegistry,
      JacksonRuntime jacksonRuntime,
      HandleByStatusCode handle,
      ChildStep childStep) {

    HandleByStatusCodeStepToKStream handler =
        new HandleByStatusCodeStepToKStream(meterRegistry, jacksonRuntime);

    ProcessorSupplier<JsonNode, JsonNode> supplier = handler.supplier(handle, childStep);

    assertThat(supplier).isNotNull();
    assertThat(supplier.get()).isNotNull();
  }

  @ParameterizedTest
  @MethodSource("paramsNull")
  void testSupplierThrowsException(
      SimpleMeterRegistry meterRegistry,
      JacksonRuntime jacksonRuntime,
      HandleByStatusCode handle,
      ChildStep childStep) {
    HandleByStatusCodeStepToKStream handler =
        new HandleByStatusCodeStepToKStream(meterRegistry, jacksonRuntime);

    ProcessorSupplier<JsonNode, JsonNode> supplier = handler.supplier(handle, childStep);

    assertThat(supplier).isNotNull();
    assertThrows(NullPointerException.class, supplier::get);
  }

  static SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
  static JacksonRuntime jacksonRuntime = new JacksonRuntime();

  static Stream<Arguments> params() {
    var handle = new HandleByStatusCode("test", HandleByStatusCodeTest.config());
    var child = new ChildStep("step");

    return Stream.of(
        Arguments.of(simpleMeterRegistry, jacksonRuntime, handle, child),
        Arguments.of(simpleMeterRegistry, jacksonRuntime, handle, null));
  }

  static Stream<Arguments> paramsNull() {
    var child = new ChildStep("step");

    return Stream.of(
        Arguments.of(simpleMeterRegistry, jacksonRuntime, null, child),
        Arguments.of(simpleMeterRegistry, jacksonRuntime, null, null));
  }
}
