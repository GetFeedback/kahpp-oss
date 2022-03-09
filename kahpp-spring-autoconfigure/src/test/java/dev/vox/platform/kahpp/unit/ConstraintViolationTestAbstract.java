package dev.vox.platform.kahpp.unit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

@SuppressWarnings({"PMD.AbstractClassWithoutAbstractMethod"})
public abstract class ConstraintViolationTestAbstract {
  protected final transient Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  protected static <T> Map<String, List<String>> validationsAsMap(
      Set<ConstraintViolation<T>> violations) {
    return violations.stream()
        .collect(
            Collectors.toMap(
                v -> v.getPropertyPath().toString(),
                v -> List.of(v.getMessage()),
                (o, o2) -> {
                  ArrayList<String> merged = new ArrayList<>();
                  merged.addAll(o);
                  merged.addAll(o2);
                  Collections.sort(merged);
                  return merged;
                }));
  }
}
