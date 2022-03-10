package dev.vox.platform.kahpp.configuration.topic.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = SourceTopicIsPresentValidator.class)
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
@Documented
public @interface SourceTopicIsPresent {

  String message() default "A 'source' topic has to be defined";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
