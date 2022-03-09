package dev.vox.platform.kahpp.unit.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeDeserializer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Streams;

class InstanceRuntimeTest {

  @BeforeEach
  @AfterEach
  void cleanInstanceRuntime() {
    InstanceRuntime.close();
  }

  @Test
  void shouldThrownExceptionWhenRuntimeWasNotInitialized() {
    assertThatThrownBy(InstanceRuntime::get).isInstanceOf(RuntimeException.class);
  }

  @Test
  void shouldCreateInstanceRuntimeOnlyOnce() {
    Instance instance =
        new Instance(
            new ConfigBuilder(
                "group", "instance", null, Map.of(), new Streams(), Map.of(), List.of()),
            new StepBuilder(List.of()));

    InstanceRuntime.init(instance.getConfig());

    assertThatThrownBy(() -> InstanceRuntime.init(instance.getConfig()))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void shouldUseMockClockInsteadOfSystemClock() {
    Instance instance =
        new Instance(
            new ConfigBuilder(
                "group", "instance", null, Map.of(), new Streams(), Map.of(), List.of()),
            new StepBuilder(List.of()));

    InstanceRuntime.init(
        instance.getConfig(),
        Clock.fixed(Instant.parse("2020-08-02T00:00:00Z"), ZoneId.systemDefault()));
    final Header header = InstanceRuntime.HeaderHelper.forSuccess(() -> "nice_step");

    JsonNode headerValue = new JsonNodeDeserializer().deserialize("header", header.value());

    assertThat(headerValue.get("timestamp").asLong()).isEqualTo(1596326400000L);
  }
}
