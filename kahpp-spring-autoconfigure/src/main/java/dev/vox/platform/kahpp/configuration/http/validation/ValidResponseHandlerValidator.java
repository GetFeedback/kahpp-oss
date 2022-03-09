package dev.vox.platform.kahpp.configuration.http.validation;

import dev.vox.platform.kahpp.configuration.http.ResponseHandlerBuilder;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerConfig;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidResponseHandlerValidator
    implements ConstraintValidator<ValidResponseHandler, ResponseHandlerConfig> {

  @Override
  public void initialize(ValidResponseHandler constraint) {}

  @Override
  public boolean isValid(
      ResponseHandlerConfig responseHandlerConfig, ConstraintValidatorContext context) {

    if (responseHandlerConfig.getType() == null) {
      context
          .buildConstraintViolationWithTemplate("The response handler type cannot be null")
          .addPropertyNode(ResponseHandlerBuilder.TYPE)
          .addConstraintViolation();

      return false;
    }

    String handlerOptionsRegex =
        String.format(
            "(%s)", String.join("|", ResponseHandlerConfig.ResponseHandlerType.valuesAsString()));
    if (!responseHandlerConfig.getType().matches(handlerOptionsRegex)) {
      context
          .buildConstraintViolationWithTemplate(
              String.format("The response handler type must match %s", handlerOptionsRegex))
          .addPropertyNode(ResponseHandlerBuilder.TYPE)
          .addConstraintViolation();

      return false;
    }

    if (ResponseHandlerConfig.ResponseHandlerType.RECORD_UPDATE
            .toString()
            .equals(responseHandlerConfig.getType())
        && responseHandlerConfig.getJmesPath() == null) {
      context
          .buildConstraintViolationWithTemplate("This response handler type needs a `jmesPath`")
          .addPropertyNode(ResponseHandlerBuilder.JMES_PATH)
          .addConstraintViolation();

      return false;
    }

    if (!ResponseHandlerConfig.ResponseHandlerType.RECORD_UPDATE
            .toString()
            .equals(responseHandlerConfig.getType())
        && responseHandlerConfig.getJmesPath() != null) {
      context
          .buildConstraintViolationWithTemplate(
              "This response handler type shouldn't have a `jmesPath`")
          .addPropertyNode(ResponseHandlerBuilder.JMES_PATH)
          .addConstraintViolation();

      return false;
    }

    if (ResponseHandlerConfig.ResponseHandlerType.RECORD_ROUTE
            .toString()
            .equals(responseHandlerConfig.getType())
        && (responseHandlerConfig.getTopics() == null
            || responseHandlerConfig.getTopics().size() == 0)) {
      context
          .buildConstraintViolationWithTemplate("This response handler type needs a `topic`")
          .addPropertyNode(ResponseHandlerBuilder.TOPICS)
          .addConstraintViolation();

      return false;
    }

    if (!ResponseHandlerConfig.ResponseHandlerType.RECORD_ROUTE
            .toString()
            .equals(responseHandlerConfig.getType())
        && (responseHandlerConfig.getTopics() != null
            && responseHandlerConfig.getTopics().size() > 0)) {
      context
          .buildConstraintViolationWithTemplate(
              "Route to a topic on this response handler type wasn't implemented yet")
          .addPropertyNode(ResponseHandlerBuilder.TOPICS)
          .addConstraintViolation();

      return false;
    }

    return true;
  }
}
