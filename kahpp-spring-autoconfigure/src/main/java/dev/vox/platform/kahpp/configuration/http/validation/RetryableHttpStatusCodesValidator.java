package dev.vox.platform.kahpp.configuration.http.validation;

import dev.vox.platform.kahpp.configuration.http.HttpClient;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class RetryableHttpStatusCodesValidator
    implements ConstraintValidator<
        RetryableHttpStatusCodes, HttpClient.Options.RetriesForHttpStatus> {

  @Override
  public void initialize(RetryableHttpStatusCodes constraint) {}

  @Override
  public boolean isValid(
      HttpClient.Options.RetriesForHttpStatus retries, ConstraintValidatorContext context) {
    AtomicBoolean isValid = new AtomicBoolean(true);

    if (retries.getStatusCode() != null
        && (retries.getStatusCodeStart() != null || retries.getStatusCodeInclusiveEnd() != null)) {
      isValid.set(false);
      context
          .buildConstraintViolationWithTemplate(
              "Define either a single status code or the start and end")
          .addPropertyNode("statusCode")
          .addConstraintViolation();
    }

    if (retries.getStatusCode() == null
        && (retries.getStatusCodeStart() == null || retries.getStatusCodeInclusiveEnd() == null)) {
      isValid.set(false);
      context
          .buildConstraintViolationWithTemplate(
              "Neither status code or a complete range is defined")
          .addPropertyNode("statusCode")
          .addConstraintViolation();
    }

    if (retries.getStatusCodeStart() != null
        && retries.getStatusCodeInclusiveEnd() != null
        && retries.getStatusCodeStart() > retries.getStatusCodeInclusiveEnd()) {
      isValid.set(false);
      context
          .buildConstraintViolationWithTemplate("Start is bigger than end")
          .addPropertyNode("statusCodeStart")
          .addConstraintViolation();
    }

    return isValid.get();
  }
}
