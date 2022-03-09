package dev.vox.platform.kahpp.integration.topic;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopicByRoute;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = ProduceToTopicByRouteTest.KStreamsTest.class)
public class ProduceToTopicByRouteTest extends AbstractKaHPPTest {

  @Test
  void recordsAreRoutedToCorrectTopics() {
    Fixture webActiveRecord = loadFixture("collection", "collection_5");
    Fixture webPassiveRecord = loadFixture("collection", "collection_6");
    sendFixture(TOPIC_SOURCE, webActiveRecord);
    sendFixture(TOPIC_SOURCE, webPassiveRecord);

    ConsumerRecords<String, String> fooRecords =
        KafkaTestUtils.getRecords(fooTopicConsumer, KAFKA_CONSUMER_TIMEOUT_SHORT);
    assertThat(fooRecords).size().isEqualTo(1);
    assertThat(fooRecords.records(TOPIC_FOO).iterator().next().key())
        .isEqualTo(webActiveRecord.getKey().trim());

    ConsumerRecords<String, String> barRecords =
        KafkaTestUtils.getRecords(barTopicConsumer, KAFKA_CONSUMER_TIMEOUT_SHORT);
    assertThat(barRecords).size().isEqualTo(1);
    assertThat(barRecords.records(TOPIC_BAR).iterator().next().key())
        .isEqualTo(webPassiveRecord.getKey().trim());

    ConsumerRecords<String, String> errorRecords =
        KafkaTestUtils.getRecords(errorTopicConsumer, KAFKA_CONSUMER_TIMEOUT_SHORT);
    assertThat(errorRecords).size().isEqualTo(0);
  }

  @Test
  void recordIsProducedToErrorTopicWhenNoRouteIsMatched() {
    Fixture sdkActiveRecord = loadFixture("collection", "collection_2");
    sendFixture(TOPIC_SOURCE, sdkActiveRecord);

    ConsumerRecords<String, String> fooRecords =
        KafkaTestUtils.getRecords(fooTopicConsumer, KAFKA_CONSUMER_TIMEOUT_SHORT);
    assertThat(fooRecords).size().isEqualTo(0);

    ConsumerRecords<String, String> barRecords =
        KafkaTestUtils.getRecords(barTopicConsumer, KAFKA_CONSUMER_TIMEOUT_SHORT);
    assertThat(barRecords).size().isEqualTo(0);

    ConsumerRecords<String, String> errorRecords =
        KafkaTestUtils.getRecords(errorTopicConsumer, KAFKA_CONSUMER_TIMEOUT_SHORT);
    assertThat(errorRecords).size().isEqualTo(1);
    assertThat(errorRecords.records(TOPIC_ERROR).iterator().next().key())
        .isEqualTo(sdkActiveRecord.getKey().trim());
  }

  @Configuration
  static class KStreamsTest extends KafkaStreamsTest {
    @Override
    protected Map<String, String> getTopics() {
      return Map.of(
          "source", AbstractKaHPPTest.TOPIC_SOURCE,
          "error", AbstractKaHPPTest.TOPIC_ERROR,
          "foo", AbstractKaHPPTest.TOPIC_FOO,
          "bar", AbstractKaHPPTest.TOPIC_BAR);
    }

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<ProduceToTopicByRoute> produceToTopicByRouteStep =
          new StepConfiguration<>(
              ProduceToTopicByRoute.class,
              "produceRecordToDynamicSinkTopic",
              Map.of(
                  "errorTopic",
                  "error",
                  "routes",
                  Map.of(
                      "0",
                          Map.of(
                              "jmesPath",
                              "value.payload.channel.name == 'collection_5'",
                              "topic",
                              "foo"),
                      "1",
                          Map.of(
                              "jmesPath",
                              "value.payload.channel.name == 'collection_6'",
                              "topic",
                              "bar"))));

      return List.of(produceToTopicByRouteStep);
    }
  }
}
