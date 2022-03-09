package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(300)
public class ConfigurationToProduceToDynamicRouteStep
    implements ConfigurationToStep<ProduceToDynamicRoute> {

  private final transient JacksonRuntime runtime;

  public ConfigurationToProduceToDynamicRouteStep(final JacksonRuntime runtime) {
    this.runtime = runtime;
  }

  @Override
  public StepConfiguration<ProduceToDynamicRoute> configure(
      StepConfiguration<ProduceToDynamicRoute> stepConfiguration, ConfigBuilder configBuilder) {
    Map<String, Object> config = new HashMap<>(stepConfiguration.getConfig());

    if (config.containsKey(ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC)) {
      config.put(
          ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC,
          new TopicIdentifier(
              config.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ERROR_TOPIC).toString()));
    }

    if (config.containsKey(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES)) {
      Object routesConfig = config.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES);
      if (routesConfig instanceof Map) {
        List<Route> routes =
            ((Map<?, ?>) routesConfig)
                .values().stream()
                    .filter(routeConfig -> routeConfig instanceof Map)
                    .map(configuration -> buildRoute((Map<?, ?>) configuration))
                    .distinct()
                    .collect(Collectors.toUnmodifiableList());
        config.put(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES, routes);
      }
    }

    return stepConfiguration.newConfig(config);
  }

  private Route buildRoute(Map<?, ?> configMap) {
    return new Route(
        runtime.compile(
            configMap.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_JMES_PATH).toString()),
        new TopicIdentifier(
            configMap.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_TOPIC).toString()));
  }

  @Override
  public Class<ProduceToDynamicRoute> supportsType() {
    return ProduceToDynamicRoute.class;
  }
}
