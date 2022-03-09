package dev.vox.platform.kahpp.unit.configuration.topic;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.topic.ProduceToStaticRoute;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

class ProduceToTopicTest extends ConstraintViolationTestAbstract {
  @Test
  public void canConstruct() {
    ProduceToTopic produceToTopic =
        new ProduceToTopic(
            "test-canConstruct",
            Map.of(
                ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC, new TopicIdentifier("sink-topic")));
    Set<ConstraintViolation<ProduceToTopic>> violations = validator.validate(produceToTopic);
    assertThat(violations).hasSize(0);
  }

  @Test
  public void canValidate() {
    ProduceToTopic produceToTopic = new ProduceToTopic("test-canValidate", Map.of("key", "value"));
    Set<ConstraintViolation<ProduceToTopic>> violations = validator.validate(produceToTopic);
    assertThat(violations).hasSize(1);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations).isEqualTo(Map.of("topic", List.of("must not be null")));
  }
}
