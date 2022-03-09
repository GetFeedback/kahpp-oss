package dev.vox.platform.kahpp.configuration.topic.validation;

import dev.vox.platform.kahpp.configuration.topic.ProduceToDynamicRoute;
import dev.vox.platform.kahpp.configuration.topic.ProduceToStaticRoute;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SinkTopicIsFoundValidator
    implements ConstraintValidator<SinkTopicIsFound, ConfigBuilder> {
  @Override
  public void initialize(SinkTopicIsFound constraintAnnotation) {}

  @Override
  public boolean isValid(ConfigBuilder builder, ConstraintValidatorContext context) {
    Set<TopicEntry.TopicIdentifier> configuredTopics = builder.getConfiguredSinkTopics();
    AtomicBoolean isValid = new AtomicBoolean(true);

    validateStaticSinkTopics(
        builder.getStaticSinkTopicsBySteps(
            ProduceToStaticRoute.class, ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC),
        configuredTopics,
        ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC,
        context,
        isValid);
    validateStaticSinkTopics(
        builder.getStaticSinkTopicsBySteps(
            ProduceToDynamicRoute.class, ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC),
        configuredTopics,
        ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC,
        context,
        isValid);
    validateDynamicSinkTopics(
        builder.getDynamicSinkTopicsBySteps(), configuredTopics, context, isValid);

    return isValid.get();
  }

  private void validateStaticSinkTopics(
      List<Map.Entry<Boolean, Optional<TopicEntry.TopicIdentifier>>> stepTopicMap,
      Set<TopicEntry.TopicIdentifier> configuredTopics,
      String propertyName,
      ConstraintValidatorContext context,
      AtomicBoolean isValid) {
    AtomicInteger index = new AtomicInteger(-1);
    stepTopicMap.stream()
        .peek(e -> index.getAndIncrement())
        .filter(Map.Entry::getKey)
        .forEach(
            entry -> {
              entry
                  .getValue()
                  .ifPresent(
                      topicIdentifier -> {
                        if (!configuredTopics.contains(topicIdentifier)) {
                          isValid.set(false);
                          context
                              .buildConstraintViolationWithTemplate(
                                  String.format(
                                      "Unmatched topic reference, unknown '%s'",
                                      topicIdentifier.asString()))
                              .addPropertyNode(String.format("steps[%s]", index.intValue()))
                              .addPropertyNode("config")
                              .addPropertyNode(propertyName)
                              .addConstraintViolation();
                        }
                      });
            });
  }

  private void validateDynamicSinkTopics(
      List<Map.Entry<Boolean, List<Optional<TopicEntry.TopicIdentifier>>>> stepTopicMap,
      Set<TopicEntry.TopicIdentifier> configuredTopics,
      ConstraintValidatorContext context,
      AtomicBoolean isValid) {
    AtomicInteger index = new AtomicInteger(-1);
    stepTopicMap.stream()
        .peek(e -> index.getAndIncrement())
        .filter(Map.Entry::getKey)
        .forEach(
            listEntry -> {
              listEntry
                  .getValue()
                  .forEach(
                      entry -> {
                        entry.ifPresent(
                            topicIdentifier -> {
                              if (!configuredTopics.contains(topicIdentifier)) {
                                isValid.set(false);
                                context
                                    .buildConstraintViolationWithTemplate(
                                        String.format(
                                            "Unmatched topic reference, unknown '%s'",
                                            topicIdentifier.asString()))
                                    .addPropertyNode(String.format("steps[%s]", index.intValue()))
                                    .addPropertyNode("config")
                                    .addPropertyNode(
                                        ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES)
                                    .addConstraintViolation();
                              }
                            });
                      });
            });
  }
}
