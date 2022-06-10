package dev.vox.platform.kahpp.unit.streams;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.filter.FilterValue;
import dev.vox.platform.kahpp.configuration.http.SimpleHttpCall;
import dev.vox.platform.kahpp.configuration.predicate.PredicateOrProduceError;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopicByRoute;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Streams;

class InstanceConfigurationValidationTest extends ConstraintViolationTestAbstract {

  @Test
  void basicValidation() {
    ConfigBuilder configBuilder =
        new ConfigBuilder("t", "b", 0, Map.of(), null, Map.of(), List.of());

    Set<ConstraintViolation<ConfigBuilder>> violations = validator.validate(configBuilder);
    assertThat(violations).hasSize(7);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "group", List.of("size must be between 2 and 20"),
                "name", List.of("size must be between 4 and 33"),
                "steps", List.of("must not be empty"),
                "streamsConfig", List.of("must not be null"),
                "topics",
                    List.of("A 'source' topic has to be defined", "size must be between 1 and 100"),
                "version", List.of("must be greater than 0")));
  }

  @Test
  void stepValidation() {
    StepConfiguration<FilterValue> httpStep =
        new StepConfiguration<>(FilterValue.class, "", Map.of());
    ConfigBuilder configBuilder =
        new ConfigBuilder(
            "test",
            "stepValidation",
            1,
            Map.of("source", "abc-topic"),
            new Streams(),
            Map.of(),
            List.of(httpStep));

    Set<ConstraintViolation<ConfigBuilder>> violations = validator.validate(configBuilder);
    assertThat(violations).hasSize(1);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations).isEqualTo(Map.of("steps[0].name", List.of("must not be blank")));
  }

  @Test
  void stepWithApiValidation() {
    StepConfiguration<SimpleHttpCall> httpStepWithoutName =
        new StepConfiguration<>(SimpleHttpCall.class, "", Map.of());
    StepConfiguration<SimpleHttpCall> httpWithWrongApi =
        new StepConfiguration<>(SimpleHttpCall.class, "my-noapi", Map.of("api", "noapi"));
    ConfigBuilder configBuilder =
        new ConfigBuilder(
            "test",
            "stepValidation",
            1,
            Map.of("source", "abc-topic"),
            new Streams(),
            Map.of(),
            List.of(httpStepWithoutName, httpWithWrongApi));

    Set<ConstraintViolation<ConfigBuilder>> violations = validator.validate(configBuilder);
    assertThat(violations).hasSize(4);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "steps[0].name", List.of("must not be blank"),
                "steps[0].config.api", List.of("Missing required Api reference"),
                "steps[1].config.api", List.of("Unmatched api reference, unknown 'noapi'"),
                "", List.of("Could not find Api reference")));
  }

  @Test
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  void stepWithTopicValidation() {
    StepConfiguration<ProduceToTopic> stepWithoutName =
        new StepConfiguration<>(ProduceToTopic.class, "", Map.of("topic", "sink"));
    StepConfiguration<PredicateOrProduceError> stepWithNonExistentTopic =
        new StepConfiguration<>(
            PredicateOrProduceError.class, "PredicateOrError", Map.of("topic", "does-not-exist"));
    StepConfiguration<ProduceToTopicByRoute> stepWithoutErrorTopic =
        new StepConfiguration<>(
            ProduceToTopicByRoute.class,
            "Dynamic",
            Map.of(
                "errorTopic",
                "does-not-exist",
                "routes",
                List.of(
                    Map.of("topic", "sink"),
                    Map.of("topic", "non-existent"),
                    Map.of("non-a-topic", "error"))));

    ConfigBuilder configBuilder =
        new ConfigBuilder(
            "test",
            "stepValidation",
            1,
            Map.of("source", "abc-topic", "sink", "sink-topic", "error", "error-topic"),
            new Streams(),
            Map.of(),
            List.of(stepWithoutName, stepWithNonExistentTopic, stepWithoutErrorTopic));

    Set<ConstraintViolation<ConfigBuilder>> violations = validator.validate(configBuilder);
    assertThat(violations).hasSize(5);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "steps[0].name", List.of("must not be blank"),
                "steps[1].config.topic",
                    List.of("Unmatched topic reference, unknown 'does-not-exist'"),
                "steps[2].config.errorTopic",
                    List.of("Unmatched topic reference, unknown 'does-not-exist'"),
                "steps[2].config.routes",
                    List.of("Unmatched topic reference, unknown 'non-existent'"),
                "", List.of("Could not find sink topic reference")));
  }
}
