package dev.vox.platform.kahpp.configuration.topic.validation;

import dev.vox.platform.kahpp.configuration.topic.TopicsMap;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SourceTopicIsPresentValidator
    implements ConstraintValidator<SourceTopicIsPresent, TopicsMap> {

  @Override
  public void initialize(SourceTopicIsPresent constraintAnnotation) {}

  @Override
  public boolean isValid(TopicsMap value, ConstraintValidatorContext context) {
    return value.containsKey(TopicsMap.TOPIC_SOURCE_IDENTIFIER);
  }
}
