package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.Connection;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings({"PMD.CloseResource", "PMD.AvoidDuplicateLiterals"})
class HttpClientConfigurationValidationTest extends ConstraintViolationTestAbstract {

  private static final Connection validConnection = new Connection(10, 20);
  private static final Options validOptions =
      new Options(validConnection, Map.of("header-key", "header-value"), null, null);

  @Test
  void connectionValidation() {
    Connection connection = new Connection(-10, 0);
    Set<ConstraintViolation<Connection>> violations = validator.validate(connection);
    assertThat(violations).hasSize(2);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "connectTimeoutMillis", List.of("must be greater than 0"),
                "socketTimeoutMs", List.of("must be greater than 0")));
  }

  @Test
  void optionsValidation() {
    Options options = new Options(validConnection, Map.of("my-empty-header", ""), null, null);
    Set<ConstraintViolation<Options>> violations = validator.validate(options);
    assertThat(violations).hasSize(1);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(Map.of("headers[my-empty-header].<map value>", List.of("must not be blank")));
  }

  @Test
  void connectionsAndOptionsValidation() {
    Connection connection = new Connection(-5, 20);
    Options options = new Options(connection, Map.of("my-other-empty-header", ""), null, null);
    Set<ConstraintViolation<Options>> violations = validator.validate(options);
    assertThat(violations).hasSize(2);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "connection.socketTimeoutMs", List.of("must be greater than 0"),
                "headers[my-other-empty-header].<map value>", List.of("must not be blank")));
  }

  @Test
  void httpClientValidation() {
    HttpClient httpClient = new HttpClient("", validOptions);
    Set<ConstraintViolation<HttpClient>> violations = validator.validate(httpClient);
    assertThat(violations).hasSize(1);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations).isEqualTo(Map.of("basePath", List.of("must not be blank")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"random-string", "localhost", "localhost:8080"})
  void httpClientBasePathValidation(String wrongUrl) {
    HttpClient httpClient = new HttpClient(wrongUrl, validOptions);
    Set<ConstraintViolation<HttpClient>> violations = validator.validate(httpClient);
    assertThat(violations).hasSize(1);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations).isEqualTo(Map.of("basePath", List.of("must be a valid URL")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://localhost",
        "http://localhost/",
        "https://a:9997",
        "http://a:9997/a",
        "https://a:9997/a/"
      })
  void httpClientValidBasePath(String validUrl) {
    HttpClient httpClient = new HttpClient(validUrl, validOptions);
    Set<ConstraintViolation<HttpClient>> violations = validator.validate(httpClient);
    assertThat(violations).hasSize(0);
  }

  @Test
  void httpClientChainValidation() {
    Connection connection = new Connection(-10, 20);
    Options options = new Options(connection, Map.of("my-empty-header", ""), null, null);
    HttpClient httpClient = new HttpClient("", options);
    Set<ConstraintViolation<HttpClient>> violations = validator.validate(httpClient);
    assertThat(violations).hasSize(3);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "basePath", List.of("must not be blank"),
                "options.headers[my-empty-header].<map value>", List.of("must not be blank"),
                "options.connection.socketTimeoutMs", List.of("must be greater than 0")));
  }
}
