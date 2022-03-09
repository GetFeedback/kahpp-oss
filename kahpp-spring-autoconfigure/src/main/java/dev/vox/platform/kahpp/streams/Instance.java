package dev.vox.platform.kahpp.streams;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.http.validation.HttpApiIsFound;
import dev.vox.platform.kahpp.configuration.topic.Produce;
import dev.vox.platform.kahpp.configuration.topic.ProduceToDynamicRoute;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.configuration.topic.TopicsMap;
import dev.vox.platform.kahpp.configuration.topic.validation.SinkTopicIsFound;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableConfigurationProperties(ConfigBuilder.class)
public class Instance {

  private final Config config;

  public Instance(ConfigBuilder configBuilder, StepBuilder toStepApplier) {
    this.config = configBuilder.build(toStepApplier);
  }

  public Config getConfig() {
    return config;
  }

  public static final class Config {
    private final String group;
    private final String name;
    private final Integer version;
    private final String applicationID;
    private final TopicsMap topics;
    @Valid private final List<Step> steps;
    private final Map<String, Object> properties;

    public Config(
        String group,
        String name,
        Integer version,
        String applicationID,
        TopicsMap topics,
        List<Step> steps,
        Map<String, Object> properties) {
      this.group = group;
      this.name = name;
      this.version = version;
      this.applicationID = applicationID;
      this.topics = topics;
      this.steps = List.copyOf(steps);
      this.properties = Map.copyOf(properties);
    }

    public String getApplicationID() {
      return applicationID;
    }

    public String getGroup() {
      return group;
    }

    public String getName() {
      return name;
    }

    public Integer getVersion() {
      return version;
    }

    public Map<String, Object> getProperties() {
      return Collections.unmodifiableMap(properties);
    }

    public TopicsMap getTopics() {
      return topics;
    }

    public List<Step> getSteps() {
      return Collections.unmodifiableList(steps);
    }
  }

  @ConstructorBinding
  @Validated
  @ConfigurationProperties(prefix = "kahpp", ignoreUnknownFields = false)
  @HttpApiIsFound
  @SinkTopicIsFound
  public static class ConfigBuilder {
    @Size(min = 2, max = 20)
    private final transient String group;

    @Size(min = 4, max = 33)
    private final transient String name;

    @Positive private final transient Integer version;
    @NotEmpty @Valid private final transient List<StepConfiguration<? extends Step>> steps;
    @Valid private final transient TopicsMap topics;
    @NotNull private final transient KafkaProperties.Streams streamsConfig;
    private final transient Map<String, @Valid HttpClient> apis;

    /**
     * The name and group size limitation comes from the fact those names will be used as k8s
     * resources, in which we have a hard limit of 63, where 53 we use here and the other 10 as
     * resource names, like configmaps, secrets, etc.
     *
     * <p>
     *
     * @param group KaHPP instance group
     * @param name KaHPP instance name
     * @param version KaHPP instance version - This version means the version of running KaHPP. This
     *     will keep the track of all KaHPP instances this item passed. Also, in the future, this
     *     data can be used to decide if we are if the current instance is reprocessing, if the
     *     current instance is a retry. All of this data will be kept in the headers of the
     *     processed item.
     * @param topics All source and sink topics for the Topology
     * @param streamsConfig KafkaStreams specific configuration
     * @param apis All Api definitions which will be used on the Http Steps
     * @param steps All defined KaHPP Steps
     */
    public ConfigBuilder(
        String group,
        String name,
        Integer version,
        Map<String, String> topics,
        KafkaProperties.Streams streamsConfig,
        Map<String, HttpClient> apis,
        List<StepConfiguration<? extends Step>> steps) {
      this.group = group;
      this.name = name;
      this.version = version != null ? version : 1;
      this.topics = new TopicsMap(topics);
      this.streamsConfig = streamsConfig;
      this.apis = apis != null ? Map.copyOf(apis) : Collections.emptyMap();
      this.steps = List.copyOf(steps);
    }

    public Set<String> getAvailableApisIdentifiers() {
      if (this.apis != null) {
        return this.apis.keySet();
      }
      return Set.of();
    }

    public Set<TopicEntry.TopicIdentifier> getConfiguredSinkTopics() {
      return this.topics.getSinkTopics().values().stream()
          .map(TopicEntry::getIdentifier)
          .collect(Collectors.toSet());
    }

    /**
     * An Entry is made of
     *
     * <p>- Boolean indicating whether the Step is assignable from {@link HttpCall}
     *
     * <p>- An Optional api identifier String
     *
     * <p>The List is this way to maintain the order of the StepConfigurations as they come, as this
     * data will be used for validation.
     */
    public List<Map.Entry<Boolean, Optional<String>>> hasTheStepAnApiEntryList() {
      return this.steps.stream()
          .map(
              config -> {
                Object apiRef = config.getConfig().get("api");
                Optional<String> optionalApi =
                    apiRef != null ? Optional.of(apiRef.toString()) : Optional.empty();
                return new SimpleEntry<>(
                    HttpCall.class.isAssignableFrom(config.getStepType()), optionalApi);
              })
          .collect(Collectors.toList());
    }

    public List<Map.Entry<Boolean, Optional<TopicEntry.TopicIdentifier>>>
        getStaticSinkTopicsBySteps(Class<? extends Produce> produceClass, String configKey) {
      return this.steps.stream()
          .map(
              config -> {
                Object topicRef = config.getConfig().get(configKey);
                Optional<TopicEntry.TopicIdentifier> optionalTopic =
                    topicRef != null
                        ? Optional.of(new TopicEntry.TopicIdentifier(topicRef.toString()))
                        : Optional.empty();
                return new SimpleEntry<>(
                    produceClass.isAssignableFrom(config.getStepType()), optionalTopic);
              })
          .collect(Collectors.toList());
    }

    public List<Map.Entry<Boolean, List<Optional<TopicEntry.TopicIdentifier>>>>
        getDynamicSinkTopicsBySteps() {
      return this.steps.stream()
          .map(
              config -> {
                Object routes =
                    config.getConfig().get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTES);
                List<Optional<TopicEntry.TopicIdentifier>> topicList = new ArrayList<>();
                if (routes instanceof List) {
                  ((List<?>) routes)
                      .forEach(
                          route ->
                              topicList.add(
                                  route instanceof Map
                                      ? extractTopicFromRouteConfig((Map<?, ?>) route)
                                      : Optional.empty()));
                }
                return new SimpleEntry<>(
                    ProduceToDynamicRoute.class.isAssignableFrom(config.getStepType()), topicList);
              })
          .collect(Collectors.toList());
    }

    private Optional<TopicEntry.TopicIdentifier> extractTopicFromRouteConfig(Map<?, ?> route) {
      Object topicRef = route.get(ProduceToDynamicRoute.STEP_CONFIGURATION_ROUTE_TOPIC);
      return topicRef != null
          ? Optional.of(new TopicEntry.TopicIdentifier(topicRef.toString()))
          : Optional.empty();
    }

    public Map<String, HttpClient> getApis() {
      return Collections.unmodifiableMap(apis);
    }

    @Valid
    public Config build(StepBuilder toStepApplier) {
      List<Step> stepConfigurations =
          steps.stream()
              .map(s -> toStepApplier.build(s, this))
              .collect(Collectors.toUnmodifiableList());

      // Override streamsConfig as necessary
      String applicationId = String.format("kahpp-%s-%s", group, name);
      streamsConfig.setApplicationId(applicationId);
      Map<String, Object> properties = streamsConfig.buildProperties();

      return new Config(
          group, name, version, applicationId, topics, stepConfigurations, properties);
    }
  }
}
