package dev.vox.platform.kahpp.configuration.http.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = RetryableHttpStatusCodesValidator.class)
@Target({TYPE})
@Retention(RUNTIME)
@Documented
public @interface RetryableHttpStatusCodes {
  String message() default "Retryable status codes wrongly configured";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
