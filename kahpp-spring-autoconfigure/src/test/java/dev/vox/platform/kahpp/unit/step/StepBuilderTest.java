package dev.vox.platform.kahpp.unit.step;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Streams;

class StepBuilderTest {

  private final transient StepBuilder stepBuilder = new StepBuilder(List.of());
  private final transient Map<Class<? extends Step>, StepConfiguration<? extends Step>>
      classStepConfigurationMap =
          Map.of(
              NoConstructorStep.class,
              new StepConfiguration<>(NoConstructorStep.class, "a", Map.of()),
              ConstructorWithWrongArgumentsStep.class,
              new StepConfiguration<>(ConstructorWithWrongArgumentsStep.class, "a", Map.of()));

  @ParameterizedTest
  @ValueSource(classes = {NoConstructorStep.class, ConstructorWithWrongArgumentsStep.class})
  public void throwsOnInvalidSteps(Class<Step> stepConfigurationClass) {
    ConfigBuilder configBuilder =
        new ConfigBuilder(
            "test",
            "throwsOnInvalidSteps",
            1,
            Map.of(),
            new Streams(),
            Map.of(),
            List.of(classStepConfigurationMap.get(stepConfigurationClass)));

    assertThatThrownBy(() -> configBuilder.build(stepBuilder)).isInstanceOf(RuntimeException.class);
  }

  private static class NoConstructorStep implements Step {
    @Override
    public String getName() {
      return "test-fake";
    }
  }

  private static class ConstructorWithWrongArgumentsStep implements Step {

    private final transient String fake;

    public ConstructorWithWrongArgumentsStep(String fake) {
      this.fake = fake;
    }

    @Override
    public String getName() {
      return fake;
    }
  }
}
