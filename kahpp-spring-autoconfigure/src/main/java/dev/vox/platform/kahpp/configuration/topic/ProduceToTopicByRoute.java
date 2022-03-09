package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProduceToTopicByRoute implements ProduceToDynamicRoute {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProduceToTopicByRoute.class);

  @NotBlank private transient String name;
  @NotNull private transient TopicIdentifier errorTopic;
  @NotEmpty private transient List<@Valid Route> routes;

  public ProduceToTopicByRoute(String name, Map<String, ?> config) {
    this.configure(name, config);
  }

  @SuppressWarnings("unchecked")
  private void configure(String name, Map<String, ?> config) {
    this.name = name;
    this.errorTopic = (TopicIdentifier) config.get(STEP_CONFIGURATION_ERROR_TOPIC);
    this.routes = (List<Route>) config.get(STEP_CONFIGURATION_ROUTES);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Set<TopicIdentifier> eligibleSinkTopics() {
    Set<TopicIdentifier> sinkTopics =
        this.routes.stream().map(Route::getTopic).collect(Collectors.toSet());
    sinkTopics.add(errorTopic);

    return sinkTopics;
  }

  @Override
  public TopicIdentifier produceToSink(KaHPPRecord record) {
    Optional<Route> matchingRoute =
        this.routes.stream()
            .filter(
                route -> {
                  boolean match = route.test(record.build());
                  LOGGER.debug(
                      "{}: Route to topic `{}` was {}",
                      getTypedName(),
                      route.getTopic().asString(),
                      match ? "matched" : "not matched");
                  return match;
                })
            .findFirst();

    if (matchingRoute.isPresent()) {
      TopicIdentifier to = matchingRoute.get().getTopic();
      LOGGER.debug("{}: Routing record to topic `{}`", getTypedName(), to.asString());

      return to;
    }

    LOGGER.warn(
        "{}: Failed to match a route, routing record to error topic: `{}`",
        getTypedName(),
        errorTopic.asString());

    return errorTopic;
  }
}
