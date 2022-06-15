package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.ConfigurationToHttpCallStep;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options;
import dev.vox.platform.kahpp.configuration.http.HttpClient.Options.Connection;
import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Streams;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
class ConfigurationToHttpCallStepTest {

  private transient ConfigurationToHttpCallStep configurationToStep;

  @BeforeEach
  void setUp() {
    configurationToStep = new ConfigurationToHttpCallStep();
  }

  @Test
  void shouldIgnoreOnMissingApiEntry() {
    ConfigBuilder configBuilder = getConfigBuilder("canProvideApiClient", Map.of());

    StepConfiguration<HttpCall> stepConfiguration = getHttpCallStepConfiguration(Map.of());

    StepConfiguration<HttpCall> updatedConfiguration =
        configurationToStep.configure(stepConfiguration, configBuilder);

    assertThat(updatedConfiguration).isSameAs(stepConfiguration);
  }

  @Test
  void shouldIgnoreOnMissingApiReference() {
    ConfigBuilder configBuilder = getConfigBuilder("canProvideApiClient", Map.of());

    Map<String, Object> stepConfig = Map.of("api", "non-existing-api");
    StepConfiguration<HttpCall> stepConfiguration = getHttpCallStepConfiguration(stepConfig);

    StepConfiguration<HttpCall> updatedConfiguration =
        configurationToStep.configure(stepConfiguration, configBuilder);

    assertThat(updatedConfiguration).isSameAs(stepConfiguration);
  }

  @Test
  void canBuildAndReuseApiClient() {
    HttpClient httpClient =
        new HttpClient("/path", new Options(new Connection(5, 1), Map.of("x", "y"), null, null));
    Map<String, HttpClient> apis = Map.of("existing-api", httpClient);
    ConfigBuilder configBuilder = getConfigBuilder("canProvideApiClient", apis);

    StepConfiguration<HttpCall> stepConfiguration =
        getHttpCallStepConfiguration(Map.of("api", "existing-api"));

    Map<String, ?> updatedConfig =
        configurationToStep.configure(stepConfiguration, configBuilder).getConfig();

    assertThat(updatedConfig).containsKeys("api", "apiClient");
    Object apiClient = updatedConfig.get("apiClient");
    assertThat(apiClient).isNotNull();
    assertThat(apiClient).isInstanceOf(ApiClient.class);

    StepConfiguration<HttpCall> newStepConfiguration =
        getHttpCallStepConfiguration(Map.of("api", "existing-api"));

    Map<String, ?> newUpdatedConfig =
        configurationToStep.configure(newStepConfiguration, configBuilder).getConfig();
    Object newApiClient = newUpdatedConfig.get("apiClient");
    assertThat(newApiClient).isNotNull();
    assertThat(newApiClient).isInstanceOf(ApiClient.class);
    assertThat(newApiClient).isSameAs(apiClient);
  }

  protected StepConfiguration<HttpCall> getHttpCallStepConfiguration(
      Map<String, Object> stepConfig) {
    return new StepConfiguration<>(HttpCall.class, "ConfigurationToHttpCallStepTest", stepConfig);
  }

  protected ConfigBuilder getConfigBuilder(String testName, Map<String, HttpClient> apis) {
    return new ConfigBuilder(
        "ConfigurationToHttpCallStepTest", testName, 1, Map.of(), new Streams(), apis, List.of());
  }
}
