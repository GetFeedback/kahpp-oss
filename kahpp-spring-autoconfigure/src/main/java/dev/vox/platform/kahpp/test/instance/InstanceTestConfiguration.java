package dev.vox.platform.kahpp.test.instance;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vox.platform.kahpp.processor.jmespath.ArrayDiffFunction;
import dev.vox.platform.kahpp.processor.jmespath.ArrayIntersectFunction;
import dev.vox.platform.kahpp.processor.jmespath.CopyKeyToPropertyFunction;
import dev.vox.platform.kahpp.processor.jmespath.NowFunction;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class InstanceTestConfiguration {
  public static final Instant CLOCK_FROZEN_INSTANT = Instant.parse("2020-08-02T10:00:00Z");
  public static final Duration CLOCK_TICK = Duration.ofSeconds(1);
  public static final Clock CLOCK_FIXED = Clock.fixed(CLOCK_FROZEN_INSTANT, ZoneId.systemDefault());

  public static final ObjectMapper MAPPER =
      JsonMapper.builder()
          .addModules(new Jdk8Module(), new JavaTimeModule())
          .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS, true)
          .build();

  @Bean
  public MeterRegistry meterRegistry() {
    MockClock mockClock = new MockClock();
    // Ensure our MockClock starts at the same time as everything else
    // Remove 1 second as that's the initial time of MockClock
    mockClock.addSeconds(CLOCK_FROZEN_INSTANT.getEpochSecond() - 1L);
    return new SimpleMeterRegistry(SimpleConfig.DEFAULT, mockClock);
  }

  @Primary
  @Bean("jacksonRuntimeTest")
  public JacksonRuntime jacksonRuntime() {
    FunctionRegistry defaultFunctions = FunctionRegistry.defaultRegistry();
    FunctionRegistry extend =
        defaultFunctions.extend(
            new ArrayIntersectFunction(),
            new ArrayDiffFunction(),
            new CopyKeyToPropertyFunction(),
            new NowFunction(CLOCK_FIXED));

    RuntimeConfiguration configuration =
        new RuntimeConfiguration.Builder().withFunctionRegistry(extend).build();

    return new JacksonRuntime(configuration);
  }
}
