package dev.vox.platform.kahpp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.KafkaStreams;
import dev.vox.platform.kahpp.KafkaStreams.TopologyBuilder;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.filter.FilterValue;
import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.configuration.meter.CounterMeter;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.transform.HeaderToValueTransform;
import dev.vox.platform.kahpp.configuration.transform.TimestampToValueTransform;
import dev.vox.platform.kahpp.configuration.transform.WrapValueTransform;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import dev.vox.platform.kahpp.streams.StepBuilderConfiguration;
import dev.vox.platform.kahpp.streams.StepBuilderConfiguration.StepBuilderMap;
import dev.vox.platform.kahpp.streams.serialization.Serdes;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@Configuration
@EnableKafkaStreams
@Import({Serdes.class, StepBuilderConfiguration.class, StepBuilder.class})
public class KafkaStreamsTest {

  public static final String SUPPORTED_PRODUCTS_JMES_FILTER =
      "payload.channel.platform == 'collection_4' || payload.channel.name == 'collection_2' || payload.channel.name == 'collection_5' || payload.channel.name == 'sdk-active' || payload.channel.name == 'web-active'";
  private static final String INSTANCE_CONFIGURATION = "instanceFunctional";

  @Bean("kafkaStreams")
  public KafkaStreams getKafkaStreams(
      @Qualifier(INSTANCE_CONFIGURATION) Instance configuration,
      @Autowired StepBuilderMap stepBuilderMap,
      @Qualifier("SerdeJsonNodeKey") Serde<JsonNode> serdeKey,
      @Qualifier("SerdeJsonNodeValue") Serde<JsonNode> serdeValue) {
    return new KafkaStreams(configuration, stepBuilderMap, serdeKey, serdeValue);
  }

  @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
  public KafkaStreamsConfiguration kStreamsConfigs(
      @Qualifier("kafkaStreams") KafkaStreams kafkaStreams) {
    return kafkaStreams.kStreamsConfigs();
  }

  @Bean
  public StreamsBuilderFactoryBeanConfigurer streamsBuilderFactoryBeanCustomizer(
      @Qualifier(INSTANCE_CONFIGURATION) Instance configuration,
      @Autowired StepBuilderMap stepBuilderMap,
      @Qualifier("SerdeJsonNodeKey") Serde<JsonNode> serdeKey,
      @Qualifier("SerdeJsonNodeValue") Serde<JsonNode> serdeValue) {
    return factoryBean ->
        factoryBean.setInfrastructureCustomizer(
            new TopologyBuilder(configuration, stepBuilderMap, serdeKey, serdeValue));
  }

  @Bean(INSTANCE_CONFIGURATION)
  public Instance getInstanceConfiguration(
      @Value("${" + EmbeddedKafkaBroker.SPRING_EMBEDDED_KAFKA_BROKERS + "}") String brokerAddresses,
      StepBuilder stepBuilder) {

    HttpClient.Options.Connection connection = new HttpClient.Options.Connection(500, 500);
    HttpClient.Options options =
        new HttpClient.Options(connection, Map.of("Accept-version", "v1"), null, null);

    HttpClient httpClient =
        new HttpClient(
            String.format("http://localhost:%s/", KaHPPMockServer.getLocalPort()), options);

    KafkaProperties.Streams streamsConfig = new KafkaProperties.Streams();
    streamsConfig.setBootstrapServers(List.of(brokerAddresses));

    return new Instance(
        new ConfigBuilder(
            "tests",
            "default",
            null,
            getTopics(),
            streamsConfig,
            Map.of("defaultApi", httpClient),
            getSteps()),
        stepBuilder);
  }

  protected Map<String, String> getTopics() {
    return Map.of(
        "source", AbstractKaHPPTest.TOPIC_SOURCE,
        "sink", AbstractKaHPPTest.TOPIC_SINK);
  }

  protected List<StepConfiguration<? extends Step>> getSteps() {
    StepConfiguration<WrapValueTransform> wrapPayload =
        new StepConfiguration<>(
            WrapValueTransform.class, "wrapPayload", Map.of("field", "payload"));

    StepConfiguration<HeaderToValueTransform> recordTransformerOperation =
        new StepConfiguration<>(
            HeaderToValueTransform.class,
            "recordTransformerOperation",
            Map.of(
                "header", "operation",
                "field", "operation"));

    StepConfiguration<TimestampToValueTransform> recordTransformerPublicationDate =
        new StepConfiguration<>(
            TimestampToValueTransform.class,
            "recordTransformerPublicationDate",
            Map.of("field", "publication_date"));

    StepConfiguration<CounterMeter> collectionPerChannel =
        new StepConfiguration<>(
            CounterMeter.class,
            "collectionPerChannel",
            Map.of("tags", Map.of("channel", "payload.channel.name")));

    StepConfiguration<FilterValue> getSupportedProducts =
        new StepConfiguration<>(
            FilterValue.class,
            "getSupportedProducts",
            Map.of("jmesPath", SUPPORTED_PRODUCTS_JMES_FILTER));

    StepConfiguration<ProduceToTopic> produceToTopicStep =
        new StepConfiguration<>(
            ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

    return List.of(
        wrapPayload,
        recordTransformerOperation,
        recordTransformerPublicationDate,
        collectionPerChannel,
        getSupportedProducts,
        produceToTopicStep);
  }

  @Bean("meterRegistry")
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
  }
}
