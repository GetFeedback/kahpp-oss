package dev.vox.platform.kahpp.configuration.http.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = ValidResponseHandlerValidator.class)
@Target({TYPE})
@Retention(RUNTIME)
@Documented
public @interface ValidResponseHandler {
  String message() default "This response handler configuration is not valid";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
