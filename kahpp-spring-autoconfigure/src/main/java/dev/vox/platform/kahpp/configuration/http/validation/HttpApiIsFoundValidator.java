package dev.vox.platform.kahpp.configuration.http.validation;

import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class HttpApiIsFoundValidator implements ConstraintValidator<HttpApiIsFound, ConfigBuilder> {
  @Override
  public void initialize(HttpApiIsFound constraint) {}

  @Override
  public boolean isValid(ConfigBuilder builder, ConstraintValidatorContext context) {
    Set<String> apis = builder.getAvailableApisIdentifiers();

    AtomicBoolean isValid = new AtomicBoolean(true);

    List<Map.Entry<Boolean, Optional<String>>> stepApiMap = builder.hasTheStepAnApiEntryList();
    Iterator<Map.Entry<Boolean, Optional<String>>> stepApiIterator = stepApiMap.iterator();

    for (int i = 0; i < stepApiMap.size(); i++) {
      Map.Entry<Boolean, Optional<String>> next = stepApiIterator.next();
      if (!next.getKey()) {
        continue;
      }

      int finalI = i;
      next.getValue()
          .ifPresentOrElse(
              s -> {
                if (!apis.contains(s)) {
                  isValid.set(false);
                  context
                      .buildConstraintViolationWithTemplate(
                          String.format("Unmatched api reference, unknown '%s'", s))
                      .addPropertyNode(String.format("steps[%s]", finalI))
                      .addPropertyNode("config")
                      .addPropertyNode("api")
                      .addConstraintViolation();
                }
              },
              () -> {
                isValid.set(false);
                // todo: The step.config should have its own type, this is a workaround
                context
                    .buildConstraintViolationWithTemplate("Missing required Api reference")
                    .addPropertyNode(String.format("steps[%s]", finalI))
                    .addPropertyNode("config")
                    .addPropertyNode("api")
                    .addConstraintViolation();
              });
    }

    return isValid.get();
  }
}
