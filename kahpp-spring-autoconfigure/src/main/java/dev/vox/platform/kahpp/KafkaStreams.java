package dev.vox.platform.kahpp;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.topic.Produce;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.configuration.topic.TopicsMap;
import dev.vox.platform.kahpp.processor.Finalize;
import dev.vox.platform.kahpp.processor.Start;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.StepBuilderConfiguration;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.Topology;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsInfrastructureCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

@Configuration
@EnableKafkaStreams
public class KafkaStreams implements AutoCloseable {

  private final transient TopologyBuilder topologyBuilder;
  private final transient Instance.Config config;

  public KafkaStreams(
      Instance instance,
      StepBuilderConfiguration.StepBuilderMap stepBuilderMap,
      @Qualifier("SerdeJsonNodeKey") Serde<JsonNode> serdeKey,
      @Qualifier("SerdeJsonNodeValue") Serde<JsonNode> serdeValue) {
    // In order to keep it standard among the whole application, streams applications won't use
    // timezones, also the kafka record timestamp is in ms UTC, while formatting dates we were
    // seeing different results on different systems, this can be a real problem in cases
    // where the same KaHPP group is deployed in multiple regions
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    this.config = instance.getConfig();
    InstanceRuntime.init(this.config);

    this.topologyBuilder = new TopologyBuilder(instance, stepBuilderMap, serdeKey, serdeValue);
  }

  @Bean("requiredKafkaStreamsTopics")
  public TopicsMap getTopics() {
    return config.getTopics();
  }

  @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
  public KafkaStreamsConfiguration kStreamsConfigs() {
    final Map<String, Object> properties = config.getProperties();

    return new KafkaStreamsConfiguration(properties);
  }

  public TopologyBuilder getTopologyBuilder() {
    return this.topologyBuilder;
  }

  @Bean
  public StreamsBuilderFactoryBeanConfigurer streamsBuilderFactoryBeanCustomizer() {
    return factoryBean -> factoryBean.setInfrastructureCustomizer(this.getTopologyBuilder());
  }

  @Override
  public void close() {
    InstanceRuntime.close();
  }

  public static class TopologyBuilder implements KafkaStreamsInfrastructureCustomizer {

    private static final String KAHPP_INTERNAL_PROCESSOR_START = "KaHPP.Start";
    private static final String KAHPP_INTERNAL_PROCESSOR_FINALIZE = "KaHPP.Finalize";

    private final transient Instance.Config config;
    private final transient StepBuilderConfiguration.StepBuilderMap stepBuilderMap;
    private final transient Serde<JsonNode> serdeKey;
    private final transient Serde<JsonNode> serdeValue;

    public TopologyBuilder(
        Instance config,
        StepBuilderConfiguration.StepBuilderMap stepBuilderMap,
        @Qualifier("SerdeJsonNodeKey") Serde<JsonNode> serdeKey,
        @Qualifier("SerdeJsonNodeValue") Serde<JsonNode> serdeValue) {
      this.config = config.getConfig();
      this.stepBuilderMap = stepBuilderMap;
      this.serdeKey = serdeKey;
      this.serdeValue = serdeValue;
    }

    @SuppressWarnings({"unchecked", "PMD.AvoidInstantiatingObjectsInLoops"})
    @Override
    public void configureTopology(Topology topology) {
      TopicsMap topics = config.getTopics();

      // During build time, keep track of all processor nodes which might point to
      // Sink nodes, it's later used to se the correct `parentNames` on them
      // Map<Topic Identifier, List of nodes that sink to it>
      Map<String, List<String>> sinkNodes =
          topics.getSinkTopics().keySet().stream()
              .collect(Collectors.toMap(topicKey -> topicKey, o -> new ArrayList<>()));

      List<Step> steps = config.getSteps();
      ListIterator<Step> iterator = steps.listIterator();

      // Mandatory `source` Topic Node
      TopicEntry sourceTopic = topics.getSource();
      final String sourceNode = sourceTopic.getIdentifier().asString();
      topology.addSource(
          sourceNode, serdeKey.deserializer(), serdeValue.deserializer(), sourceTopic.getName());

      // KaHPP internal processor, adds MDC context and logs
      topology.addProcessor(KAHPP_INTERNAL_PROCESSOR_START, () -> new Start(config), sourceNode);

      while (iterator.hasNext()) {
        // As every processor node needs:
        //
        // - The parentName, we use the previous user configured Step
        // - A child node to forward messages to, we look up for the next Step,
        // if there's no user configured next Step, point to KAHPP_INTERNAL_PROCESSOR_FINALIZE
        // which will write the final logs and finish the stream
        // - The current Step in order to build the ProcessorSupplier
        //
        // We'll work on a functional way to retrieve this information and set the necessary
        // defaults when needed
        String parentNode =
            Try.of(() -> Optional.of(steps.get(iterator.previousIndex())))
                .getOrElse(Optional.empty())
                .map(Step::getTypedName)
                .orElse(KAHPP_INTERNAL_PROCESSOR_START);

        Step currentStep = iterator.next();

        ChildStep child =
            Try.of(() -> Optional.of(steps.get(iterator.nextIndex())))
                .getOrElse(Optional.empty())
                .map(ChildStep::new)
                .orElse(new ChildStep(KAHPP_INTERNAL_PROCESSOR_FINALIZE));

        StepProcessorSupplier<? extends Step> stepToKStream = stepBuilderMap.get(currentStep);

        // The current user defined Step can now be added as a Processor Node
        topology.addProcessor(
            currentStep.getTypedName(),
            ((StepProcessorSupplier<? super Step>) stepToKStream).supplier(currentStep, child),
            parentNode);

        // In case the Step is a Producer, update the sinkNode Map Accordingly
        if (currentStep instanceof Produce) {
          ((Produce) currentStep)
              .eligibleSinkTopics()
              .forEach(
                  topicIdentifier ->
                      sinkNodes.computeIfPresent(
                          topicIdentifier.asString(),
                          (topic, strings) -> {
                            strings.add(currentStep.getTypedName());
                            return strings;
                          }));
        }
      }

      // Iterates over all the user configured Sink Topics, creates a sink processor node
      // and based on the sinkNodes Map, reference to every possible parent Node thus
      // they're able to write their records
      topics
          .getSinkTopics()
          .forEach(
              (identifier, topic) ->
                  topology.addSink(
                      topic.getIdentifier().asString(),
                      topic.getName(),
                      serdeKey.serializer(),
                      serdeValue.serializer(),
                      sinkNodes.get(identifier).toArray(new String[0])));

      // Adds the terminal Processor Node and point to the previous known user defined Step
      // This is the happy path final Step
      topology.addProcessor(
          KAHPP_INTERNAL_PROCESSOR_FINALIZE, Finalize::new, iterator.previous().getTypedName());
    }
  }
}
