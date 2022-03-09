package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.HashMap;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(300)
public class ConfigurationToProduceToStaticRouteStep
    implements ConfigurationToStep<ProduceToStaticRoute> {

  @Override
  public StepConfiguration<ProduceToStaticRoute> configure(
      StepConfiguration<ProduceToStaticRoute> stepConfiguration, ConfigBuilder configBuilder) {
    HashMap<String, Object> config = new HashMap<>(stepConfiguration.getConfig());

    if (config.containsKey(ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC)) {
      Object identifier = config.get(ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC);
      if (!(identifier instanceof TopicIdentifier)) {
        config.put(
            ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC,
            new TopicIdentifier(identifier.toString()));
      }
    }

    return stepConfiguration.newConfig(config);
  }

  @Override
  public Class<ProduceToStaticRoute> supportsType() {
    return ProduceToStaticRoute.class;
  }
}
