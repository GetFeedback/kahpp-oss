package dev.vox.platform.kahpp.unit.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.configuration.topic.TopicsMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TopicsMapTest {
  private transient Validator validator;

  @BeforeEach
  @SuppressWarnings("PMD.CloseResource")
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void getInvalidIdentifierShouldFail() {
    final TopicsMap topicsMap = new TopicsMap();
    assertThatThrownBy(
            () -> topicsMap.get("someTopic"), "Could not find Topic with identifier: \"someTopic\"")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getAll() {
    TopicsMap topics =
        new TopicsMap(
            Map.of(
                "x", "a",
                "y", "b",
                "z", "c"));
    assertThat(topics.all()).isEqualTo(Set.of("a", "b", "c"));
  }

  @Test
  void hasAtLeastOneTopic() {
    TopicsMap topics = new TopicsMap();
    Set<ConstraintViolation<TopicsMap>> validate = validator.validate(topics);
    assertThat(validate).hasSize(2);
    final Set<String> messages =
        validate.stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toUnmodifiableSet());
    assertThat(messages).contains("size must be between 1 and 100");
    assertThat(messages).contains("A 'source' topic has to be defined");
  }

  @Test
  void sourceTopicIsPresent() {
    TopicsMap topics = new TopicsMap(Map.of("x", "a"));
    Set<ConstraintViolation<TopicsMap>> validate = validator.validate(topics);
    assertThat(validate).hasSize(1);
    assertThat(validate.iterator().next().getMessage())
        .isEqualTo("A 'source' topic has to be defined");
  }

  @Test
  void canSet() {
    TopicsMap topics =
        new TopicsMap(
            Map.of(
                "source", "a",
                "sink", "b",
                "error", "c"));

    Set<ConstraintViolation<TopicsMap>> validate = validator.validate(topics);
    assertThat(validate).hasSize(0);

    Set<String> expected =
        Set.of(
            topics.getSource().getName(),
            topics.get("sink").getName(),
            topics.get("error").getName());
    assertThat(topics.all()).isEqualTo(expected);
  }
}
