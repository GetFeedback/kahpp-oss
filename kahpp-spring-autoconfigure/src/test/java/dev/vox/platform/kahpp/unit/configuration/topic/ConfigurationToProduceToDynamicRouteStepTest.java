package dev.vox.platform.kahpp.unit.configuration.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.configuration.topic.ConfigurationToProduceToDynamicRouteStep;
import dev.vox.platform.kahpp.configuration.topic.ProduceToDynamicRoute;
import dev.vox.platform.kahpp.configuration.topic.Route;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.burt.jmespath.parser.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

class ConfigurationToProduceToDynamicRouteStepTest {
  private transient ConfigurationToProduceToDynamicRouteStep configurationToStep;
  private transient JacksonRuntime runtime;

  @BeforeEach
  void setUp() {
    runtime = new JacksonRuntime();
    configurationToStep = new ConfigurationToProduceToDynamicRouteStep(runtime);
  }

  @Test
  void shouldNotUpdateOnMissingConfiguration() {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(Map.of());

    Map<String, ?> updatedConfig =
        configurationToStep.configure(stepConfiguration, configBuilder).getConfig();

    assertThat(updatedConfig).isEqualTo(stepConfiguration.getConfig());
  }

  @Test
  void shouldAddErrorTopicWhenPresentInConfiguration() {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC, "error-topic"));

    Map<String, ?> updatedConfig =
        configurationToStep.configure(stepConfiguration, configBuilder).getConfig();

    assertThat(updatedConfig).containsKey(ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC);
    assertThat(updatedConfig.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC))
        .isEqualTo(new TopicIdentifier("error-topic"));
  }

  @ParameterizedTest
  @MethodSource("provideInvalidRouteConfigurations")
  void shouldSkipRoutesWhenOfIncorrectDatatypeInConfiguration(Object routeConfiguration) {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES, routeConfiguration));

    Map<String, ?> updatedConfig =
        configurationToStep.configure(stepConfiguration, configBuilder).getConfig();

    assertThat(updatedConfig).isEqualTo(stepConfiguration.getConfig());
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideInvalidRouteConfigurations() {
    return Stream.of("routes", List.of("some", "more", "data"));
  }

  @Test
  void shouldSkipRouteWhenNotOfCorrectShapeInConfiguration() {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES, Map.of("key", "value")));

    Map<String, ?> updatedConfig =
        configurationToStep.configure(stepConfiguration, configBuilder).getConfig();

    assertThat(updatedConfig).containsKey(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES);
    assertThat(updatedConfig.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES))
        .isEqualTo(List.of());
  }

  @Test
  void shouldThrowExceptionWhenPredicateIsNotPresentInRouteConfiguration() {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(
                ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES,
                Map.of(
                    "0",
                    Map.of(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_TOPIC, "sink-topic"))));

    assertThatThrownBy(() -> configurationToStep.configure(stepConfiguration, configBuilder))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowExceptionWhenJmesPathCompilationFails() {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(
                ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES,
                Map.of(
                    "0",
                    Map.of(
                        ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_JMES_PATH,
                        "not an expression",
                        ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_TOPIC,
                        "topic"))));

    assertThatThrownBy(() -> configurationToStep.configure(stepConfiguration, configBuilder))
        .isInstanceOf(ParseException.class);
  }

  @Test
  void shouldThrowExceptionWhenTopicIsNotPresentInRouteConfiguration() {
    ConfigBuilder configBuilder = getConfigBuilder();

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(
                ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES,
                Map.of(
                    "0",
                    Map.of(
                        ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_JMES_PATH,
                        "path == 'value'"))));

    assertThatThrownBy(() -> configurationToStep.configure(stepConfiguration, configBuilder))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldRemoveDuplicateRoutes() {
    ConfigBuilder configBuilder = getConfigBuilder();

    Map<String, String> routeConfiguration =
        Map.of(
            ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_JMES_PATH,
            "path == 'value'",
            ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_TOPIC,
            "sink");

    StepConfiguration<ProduceToDynamicRoute> stepConfiguration =
        getProduceToDynamicRouteStepConfiguration(
            Map.of(
                ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES,
                Map.of(
                    "0", routeConfiguration,
                    "1", routeConfiguration)));

    Map<String, ?> updatedConfig =
        configurationToStep.configure(stepConfiguration, configBuilder).getConfig();

    assertThat(updatedConfig).containsKey(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES);
    assertThat(updatedConfig.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES))
        .isEqualTo(
            List.of(new Route(runtime.compile("path == 'value'"), new TopicIdentifier("sink"))));
  }

  protected StepConfiguration<ProduceToDynamicRoute> getProduceToDynamicRouteStepConfiguration(
      Map<String, Object> stepConfig) {
    return new StepConfiguration<>(
        ProduceToDynamicRoute.class, "ConfigurationToHttpCallStepTest", stepConfig);
  }

  protected ConfigBuilder getConfigBuilder() {
    return new ConfigBuilder(
        "KaHPP-group",
        "KaHPP-name",
        1,
        Map.of(),
        new KafkaProperties.Streams(),
        Map.of(),
        List.of());
  }
}
