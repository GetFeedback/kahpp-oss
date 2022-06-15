package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.http.OkOrProduceError;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerRecordUpdate;
import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

class OkOrProduceErrorTest extends ConstraintViolationTestAbstract {

  @Test
  void canConstruct() {
    OkOrProduceError step =
        new OkOrProduceError(
            "test-canConstruct",
            Map.of(
                "method",
                "put",
                "path",
                "/",
                "apiClient",
                new ApiClient.Builder("https://localhost").setRequestConfig(10, 20).build(),
                "topic",
                new TopicEntry.TopicIdentifier("error"),
                "forwardRecordOnError",
                "true",
                "condition",
                Condition.ALWAYS,
                "responseHandler",
                ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE));

    Set<ConstraintViolation<OkOrProduceError>> violations = validator.validate(step);
    assertThat(violations).hasSize(0);
  }

  @Test
  void canValidate() {
    OkOrProduceError step =
        new OkOrProduceError(
            "test-canValidate",
            Map.of(
                "method", "invalid",
                "transformRecord", "maybe",
                "forwardRecordOnError", "notSureYet"));

    Set<ConstraintViolation<OkOrProduceError>> violations = validator.validate(step);
    assertThat(violations).hasSize(7);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "forwardRecordOnError", List.of("must match \"true|false\""),
                "path", List.of("must not be blank"),
                "apiClient",
                    List.of("Could not find apiClient, possibly missing entry in `kahpp.apis`"),
                "topic", List.of("must not be null"),
                "method", List.of("must match \"POST|PUT|PATCH\""),
                "condition", List.of("must not be null"),
                "responseHandler", List.of("must not be null")));
  }
}
