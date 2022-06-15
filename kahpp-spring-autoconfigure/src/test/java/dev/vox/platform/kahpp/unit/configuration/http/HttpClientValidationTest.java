package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.Retries;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.RetriesForHttpStatus;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class HttpClientValidationTest extends ConstraintViolationTestAbstract {

  public static final String MUST_BE_GREATER_THAN_0 = "must be greater than 0";

  @Test
  void retriesBasicValidation() {
    Retries retries =
        new Retries(List.of(new RetriesForHttpStatus(500, 500, 500, 5)), 0, null, null, 0, 0, 0);

    Set<ConstraintViolation<Retries>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "connectionRetryCount", List.of(MUST_BE_GREATER_THAN_0),
                "statusCodeRetryTimeCapInMs", List.of(MUST_BE_GREATER_THAN_0),
                "statusCodeRetryTimeSeedInMs", List.of(MUST_BE_GREATER_THAN_0),
                "statusCodeRetryMemory", List.of(MUST_BE_GREATER_THAN_0),
                "statusCodes[0]", List.of("Retryable status codes wrongly configured"),
                "statusCodes[0].statusCode",
                    List.of("Define either a single status code or the start and end")));
  }

  @Test
  void retriesForHttpStatusWithRangeBasicValidation() {
    RetriesForHttpStatus retries = new RetriesForHttpStatus(null, 0, 0, 0);

    Set<ConstraintViolation<RetriesForHttpStatus>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "statusCodeStart", List.of(MUST_BE_GREATER_THAN_0),
                "statusCodeInclusiveEnd", List.of(MUST_BE_GREATER_THAN_0),
                "retries", List.of(MUST_BE_GREATER_THAN_0)));
  }

  @Test
  void retriesForHttpStatusWithSingleCodeBasicValidation() {
    RetriesForHttpStatus retries = new RetriesForHttpStatus(0, null, null, 0);

    Set<ConstraintViolation<RetriesForHttpStatus>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "statusCode", List.of(MUST_BE_GREATER_THAN_0),
                "retries", List.of(MUST_BE_GREATER_THAN_0)));
  }

  @Test
  void retriesForHttpStatusWithCodeAndRange() {
    RetriesForHttpStatus retries = new RetriesForHttpStatus(500, 500, 503, 3);

    Set<ConstraintViolation<RetriesForHttpStatus>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(violations).hasSize(2);
    assertThat(actualViolations.get("statusCode").get(0))
        .isEqualTo("Define either a single status code or the start and end");
  }

  @Test
  void retriesForHttpStatusRangeWhereEndIsBiggerThanStart() {
    RetriesForHttpStatus retries = new RetriesForHttpStatus(null, 501, 500, 3);

    Set<ConstraintViolation<RetriesForHttpStatus>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(violations).hasSize(2);
    assertThat(actualViolations.get("statusCodeStart").get(0))
        .isEqualTo("Start is bigger than end");
  }

  @Test
  void retriesForHttpStatusRangeWhereStartIsNotDefined() {
    RetriesForHttpStatus retries = new RetriesForHttpStatus(null, null, 500, 3);

    Set<ConstraintViolation<RetriesForHttpStatus>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(violations).hasSize(2);
    assertThat(actualViolations.get("statusCode").get(0))
        .isEqualTo("Neither status code or a complete range is defined");
  }

  @Test
  void retriesForHttpStatusRangeWhereEndIsNotDefined() {
    RetriesForHttpStatus retries = new RetriesForHttpStatus(null, 500, null, 3);

    Set<ConstraintViolation<RetriesForHttpStatus>> violations = validator.validate(retries);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(violations).hasSize(2);
    assertThat(actualViolations.get("statusCode").get(0))
        .isEqualTo("Neither status code or a complete range is defined");
  }
}
