package dev.vox.platform.kahpp.configuration.http;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.configuration.util.Range;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class HandleByStatusCodeTest extends ConstraintViolationTestAbstract {

  static TopicIdentifier topicIdentifier = new TopicIdentifier("my-topic");
  static ApiClient apiClient = new ApiClient.Builder("/path").setRequestConfig(1, 1).build();

  @Test
  public void failsValidationOnEmptyMap() {
    var invalidHandler = new HandleByStatusCode("test", emptyMap());
    var validate = validator.validate(invalidHandler);
    assertThat(validate).isNotEmpty();
    List<String> invalidProperties =
        validate.stream()
            .map(
                handleByStatusCodeConstraintViolation ->
                    handleByStatusCodeConstraintViolation.getPropertyPath().toString())
            .collect(Collectors.toList());
    assertThat(invalidProperties).contains("path", "apiClient", "responseHandler", "topic");
  }

  static Map<String, Object> config() {
    Map<String, Object> config = new HashMap<>();
    config.put("path", "/path");
    config.put("method", "POST");
    config.put("forwardRecordOnError", "error-topic");
    config.put("topic", topicIdentifier);
    config.put("apiClient", apiClient);
    config.put(
        "responseHandlers", Map.of(new Range(200, 299), ResponseHandler.RECORD_FORWARD_AS_IS));
    return config;
  }

  @Test
  void shouldListAllTopics() {
    var handle = new HandleByStatusCode("test", HandleByStatusCodeTest.config());
    assertThat(handle.eligibleSinkTopics()).contains(topicIdentifier);
  }
}
