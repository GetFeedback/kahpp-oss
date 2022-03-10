package dev.vox.platform.kahpp.integration.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.filter.FilterValue;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.transform.KeyTransform;
import dev.vox.platform.kahpp.configuration.transform.RecordTransformStepToKStream;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = RecordTransformTest.KStreamsTest.class)
public class RecordTransformTest extends AbstractKaHPPTest {

  @Test
  void feedbackKeyIsWrappedPayloadId() {
    sendCollectionFeedbackItemFixture("collection_5");

    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(record.key())
        .isEqualTo("{\"feedback_id\":\"6c34e371-4a5d-4730-b72f-67efe9d36ccb\"}");
  }

  @Test
  void recordTransformedKeyIsUpdatedOnMDC() {
    ListAppender<ILoggingEvent> listAppender = creteLogListAppender();

    final Fixture fixture = sendCollectionFeedbackItemFixture("collection_5");

    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(listAppender.list.size()).isEqualTo(2);
    assertThat(listAppender.list.get(1).getFormattedMessage())
        .isEqualTo(
            String.format(
                "KeyTransform.setResponseIdAsKey: changed Record key from `%s` to `%s`",
                fixture.getKey().trim(), record.key()));
  }

  @Test
  void recordBaseKeyStayOnMDCWhenItIsNotTransformed() {
    ListAppender<ILoggingEvent> listAppender = creteLogListAppender();

    sendCollectionFeedbackItemFixture("collection_6");

    assertThatThrownBy(
            () ->
                KafkaTestUtils.getSingleRecord(
                    sinkTopicConsumer, TOPIC_SINK, KAFKA_CONSUMER_TIMEOUT_SHORT))
        .isInstanceOf(IllegalStateException.class);

    assertThat(listAppender.list.size()).isEqualTo(0);
  }

  private ListAppender<ILoggingEvent> creteLogListAppender() {
    Logger recordTransformLogger =
        (Logger) LoggerFactory.getLogger(RecordTransformStepToKStream.class);

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    recordTransformLogger.addAppender(listAppender);

    return listAppender;
  }

  @Configuration
  public static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<FilterValue> getSupportedProducts =
          new StepConfiguration<>(
              FilterValue.class,
              "keepSupportedProducts",
              Map.of("jmesPath", SUPPORTED_PRODUCTS_JMES_FILTER));

      StepConfiguration<KeyTransform> setResponseIdAsKey =
          new StepConfiguration<>(
              KeyTransform.class,
              "setResponseIdAsKey",
              Map.of("jmesPath", "value.payload.id", "wrapInField", "feedback_id"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(getSupportedProducts, setResponseIdAsKey, produceToTopicStep);
    }
  }
}
