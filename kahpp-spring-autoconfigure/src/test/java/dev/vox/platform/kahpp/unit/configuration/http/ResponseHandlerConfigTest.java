package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.http.ResponseHandler;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerConfig;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerConfig.ResponseHandlerType;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerRecordUpdate;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ResponseHandlerConfigTest extends ConstraintViolationTestAbstract {

  @Test
  void responseHandlerTypeShouldNotAcceptNull() {
    ResponseHandlerConfig responseHandlerConfig = new ResponseHandlerConfig(null, null, null);

    assertResponseHandlerViolations(
        responseHandlerConfig, "type", "The response handler type cannot be null");
  }

  @Test
  void responseHandlerTypeShouldValidateExpectedTypes() {
    ResponseHandlerConfig responseHandlerConfig = new ResponseHandlerConfig("INVALID", null, null);

    assertResponseHandlerViolations(
        responseHandlerConfig,
        "type",
        "The response handler type must match ("
            + "RECORD_FORWARD_AS_IS|RECORD_VALUE_REPLACE|RECORD_UPDATE|RECORD_TERMINATE|RECORD_ROUTE)");
  }

  @Test
  void responseHandlerTypeRecordUpdateMustHaveJmesPath() {
    ResponseHandlerConfig responseHandlerConfig =
        new ResponseHandlerConfig(ResponseHandlerType.RECORD_UPDATE.toString(), null, null);

    assertResponseHandlerViolations(
        responseHandlerConfig, "jmesPath", "This response handler type needs a `jmesPath`");
  }

  @Test
  void responseHandlerTypeRecordReplaceMustNotHaveAJmesPath() {
    ResponseHandlerConfig responseHandlerConfig =
        new ResponseHandlerConfig(
            ResponseHandlerType.RECORD_VALUE_REPLACE.toString(), "jmes.path", null);

    assertResponseHandlerViolations(
        responseHandlerConfig,
        "jmesPath",
        "This response handler type shouldn't have a `jmesPath`");
  }

  @Test
  void responseHandlerTypeNoOpMustNotHaveAJmesPath() {
    ResponseHandlerConfig responseHandlerConfig =
        new ResponseHandlerConfig(
            ResponseHandlerType.RECORD_FORWARD_AS_IS.toString(), "jmes.path", null);

    assertResponseHandlerViolations(
        responseHandlerConfig,
        "jmesPath",
        "This response handler type shouldn't have a `jmesPath`");
  }

  @Test
  void responseHandlerTypeRecordRouteMustNotHaveAJmesPath() {
    ResponseHandlerConfig responseHandlerConfig =
        new ResponseHandlerConfig(
            ResponseHandlerType.RECORD_ROUTE.toString(), "jmes.path", Set.of("topic1", "topic2"));

    assertResponseHandlerViolations(
        responseHandlerConfig,
        "jmesPath",
        "This response handler type shouldn't have a `jmesPath`");
  }

  @Test
  void responseHandlerTypeRecordRouteMustHaveATopic() {
    ResponseHandlerConfig responseHandlerConfig =
        new ResponseHandlerConfig(ResponseHandlerType.RECORD_ROUTE.toString(), null, null);

    assertResponseHandlerViolations(
        responseHandlerConfig, "topics", "This response handler type needs a `topic`");
  }

  @Test
  void responseHandlerTypeRecordReplaceMustNotHaveARouteToTopic() {
    ResponseHandlerConfig responseHandlerConfig =
        new ResponseHandlerConfig(
            ResponseHandlerType.RECORD_VALUE_REPLACE.toString(),
            null,
            Collections.singleton("topic"));

    assertResponseHandlerViolations(
        responseHandlerConfig,
        "topics",
        "Route to a topic on this response handler type wasn't implemented yet");
  }

  @Test
  void invalidHandleTypeShouldFail() {
    ResponseHandlerConfig shouldFail = new ResponseHandlerConfig("FAKE_TYPE", "", null);

    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(shouldFail::build);
  }

  @Test
  void buildValidConfigs() {
    ResponseHandlerConfig noOp =
        new ResponseHandlerConfig(ResponseHandlerType.RECORD_FORWARD_AS_IS.toString(), null, null);
    assertThat(validationsAsMap(validator.validate(noOp))).isEmpty();
    assertThat(noOp.build()).isEqualTo(ResponseHandler.RECORD_FORWARD_AS_IS);

    ResponseHandlerConfig valueReplace =
        new ResponseHandlerConfig(ResponseHandlerType.RECORD_VALUE_REPLACE.toString(), null, null);
    assertThat(validationsAsMap(validator.validate(valueReplace))).isEmpty();
    assertThat(valueReplace.build()).isEqualTo(ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE);

    ResponseHandlerConfig recordUpdate =
        new ResponseHandlerConfig(
            ResponseHandlerType.RECORD_UPDATE.toString(), "awesome.jmesPath", null);
    assertThat(validationsAsMap(validator.validate(recordUpdate))).isEmpty();
    recordUpdate.build();

    ResponseHandlerConfig recordRoute =
        new ResponseHandlerConfig(
            ResponseHandlerType.RECORD_ROUTE.toString(), null, Set.of("myTopic1", "myTopic2"));
    assertThat(validationsAsMap(validator.validate(recordRoute))).isEmpty();
    recordRoute.build();
  }

  @Test
  void forwardAsIsShouldReturnNoTransformation() {
    final ResponseHandler asIs = ResponseHandler.RECORD_FORWARD_AS_IS;

    assertThatCode(
            () -> {
              RecordAction action = asIs.handle(null);
              assertThat(action.shouldForward()).isTrue();
              assertThat(action).isNotInstanceOf(TransformRecord.class);
            })
        .doesNotThrowAnyException();
  }

  private void assertResponseHandlerViolations(
      ResponseHandlerConfig responseHandlerConfig, String type, String s) {
    Set<ConstraintViolation<ResponseHandlerConfig>> violations =
        validator.validate(responseHandlerConfig);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);

    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "", List.of("This response handler configuration is not valid"), type, List.of(s)));
  }

  @Test
  void recordTerminate() {
    var config = new ResponseHandlerConfig("RECORD_TERMINATE", null, null);
    ResponseHandler handler = config.build();
    assertThat(handler).isEqualTo(ResponseHandler.RECORD_TERMINATE);
  }
}
