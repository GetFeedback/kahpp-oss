package dev.vox.platform.kahpp.configuration.topic.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = SinkTopicIsFoundValidator.class)
@Target({TYPE})
@Retention(RUNTIME)
@Documented
public @interface SinkTopicIsFound {
  String message() default "Could not find sink topic reference";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
