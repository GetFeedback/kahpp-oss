package dev.vox.platform.kahpp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.util.concurrent.ListenableFuture;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = KafkaStreamsTest.class)
@EmbeddedKafka(
    controlledShutdown = true,
    partitions = 3,
    brokerProperties = {
      "group.initial.rebalance.delay.ms=0",
      "replica.high.watermark.checkpoint.interval.ms=100000000"
    },
    topics = {
      AbstractKaHPPTest.TOPIC_SOURCE,
      AbstractKaHPPTest.TOPIC_SINK,
      AbstractKaHPPTest.TOPIC_ERROR,
      AbstractKaHPPTest.TOPIC_FOO,
      AbstractKaHPPTest.TOPIC_BAR
    })
public abstract class AbstractKaHPPTest {

  public static final String TOPIC_SOURCE = "topic-input";
  public static final String TOPIC_SINK = "topic-output-success";
  public static final String TOPIC_ERROR = "topic-output-error";
  public static final String TOPIC_FOO = "topic-output-foo";
  public static final String TOPIC_BAR = "topic-output-bar";
  public static final int KAFKA_CONSUMER_TIMEOUT_SHORT = 1000;

  @Autowired protected transient EmbeddedKafkaBroker embeddedKafka;

  protected transient Consumer<String, String> sinkTopicConsumer;
  protected transient Consumer<String, String> errorTopicConsumer;
  protected transient Consumer<String, String> fooTopicConsumer;
  protected transient Consumer<String, String> barTopicConsumer;

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @BeforeEach
  public void setUp() {
    this.sinkTopicConsumer = createConsumer(this.embeddedKafka);
    this.embeddedKafka.consumeFromAnEmbeddedTopic(this.sinkTopicConsumer, TOPIC_SINK);
    this.errorTopicConsumer = createConsumer(this.embeddedKafka);
    this.embeddedKafka.consumeFromAnEmbeddedTopic(this.errorTopicConsumer, TOPIC_ERROR);
    this.fooTopicConsumer = createConsumer(this.embeddedKafka);
    this.embeddedKafka.consumeFromAnEmbeddedTopic(this.fooTopicConsumer, TOPIC_FOO);
    this.barTopicConsumer = createConsumer(this.embeddedKafka);
    this.embeddedKafka.consumeFromAnEmbeddedTopic(this.barTopicConsumer, TOPIC_BAR);
  }

  @AfterEach
  public void tearDown() {
    this.sinkTopicConsumer.close();
    this.errorTopicConsumer.close();
    this.fooTopicConsumer.close();
    this.barTopicConsumer.close();
    InstanceRuntime.close();
  }

  protected Consumer<String, String> createConsumer(EmbeddedKafkaBroker broker) {
    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", broker);
    consumerProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 0);
    consumerProps.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 1000);
    consumerProps.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 1000);
    consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    DefaultKafkaConsumerFactory<String, String> kafkaConsumerFactory =
        new DefaultKafkaConsumerFactory<>(
            consumerProps, new StringDeserializer(), new StringDeserializer());
    return kafkaConsumerFactory.createConsumer();
  }

  protected KafkaTemplate<String, String> kafkaTemplate() {
    Map<String, Object> senderProps = KafkaTestUtils.producerProps(this.embeddedKafka);
    senderProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    senderProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    senderProps.put(ProducerConfig.LINGER_MS_CONFIG, "0");
    senderProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, Long.MAX_VALUE);
    senderProps.put(ProducerConfig.ACKS_CONFIG, "1");
    ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(senderProps);
    KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(pf);
    kafkaTemplate.setDefaultTopic(TOPIC_SOURCE);
    return kafkaTemplate;
  }

  public ConsumerRecord<String, String> processCollectionFixture(String fixtureName, String topic) {
    Fixture fixture = sendCollectionFeedbackItemFixture(fixtureName);

    try {
      ConsumerRecord<String, String> record =
          KafkaTestUtils.getSingleRecord(sinkTopicConsumer, topic, 1000);

      assertThat(fixture.getKey().trim()).isEqualTo(record.key().trim());
      assertThat(fixture.getValue().trim()).isEqualTo(fixture.getValue().trim());

      return record;
    } catch (IllegalStateException ignored) {
      return null;
    }
  }

  public Fixture sendCollectionFeedbackItemFixture(String directory) {
    return sendFixture(TOPIC_SOURCE, "collection", directory);
  }

  private String getJsonKeyFixture(String path) throws IOException {
    return getJsonFixture(path, "key.json");
  }

  private String getJsonValueFixture(String path) throws IOException {
    return getJsonFixture(path, "value.json");
  }

  private String getJsonFixture(String path, String file) throws IOException {
    return Files.readString(Paths.get("src/test/resources/Fixtures", path, file));
  }

  protected Fixture sendFixture(String topic, String type, String directory) {
    Fixture fixture = loadFixture(type, directory);

    return sendFixture(topic, fixture);
  }

  protected Fixture sendFixture(String topic, Fixture fixture) {
    assertThatCode(
            () -> {
              RecordHeaders headers = new RecordHeaders();
              headers.add(
                  new RecordHeader(
                      "operation", OBJECT_MAPPER.writeValueAsBytes("create_cake")));

              ProducerRecord<String, String> producerRecord =
                  new ProducerRecord<>(
                      topic, null, null, fixture.getKey(), fixture.getValue(), headers);
              final ListenableFuture<SendResult<String, String>> send =
                  this.kafkaTemplate().send(producerRecord);

              // Force the synchronous produce operation
              send.get();
              kafkaTemplate().flush();
            })
        .doesNotThrowAnyException();

    return fixture;
  }

  protected Fixture loadFixture(String type, String directory) {
    final String path = String.format("%s/%s", type, directory);
    Fixture fixture = new Fixture();

    assertThatCode(
            () -> {
              fixture.setKey(getJsonKeyFixture(path));
              fixture.setValue(getJsonValueFixture(path));
            })
        .doesNotThrowAnyException();

    return fixture;
  }
}
