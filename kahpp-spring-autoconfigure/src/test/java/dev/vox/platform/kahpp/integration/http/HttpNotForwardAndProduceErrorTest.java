package dev.vox.platform.kahpp.integration.http;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.http.OkOrProduceError;
import dev.vox.platform.kahpp.configuration.topic.ProduceToStaticRoute;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.integration.KaHPPMockServer;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = HttpNotForwardAndProduceErrorTest.KStreamsTest.class)
public class HttpNotForwardAndProduceErrorTest extends AbstractKaHPPTest {

  @BeforeAll
  static void setupMockServer() {
    KaHPPMockServer.initServer();
  }

  @AfterAll
  static void stopMockServer() {
    KaHPPMockServer.closeServer();
  }

  @Test
  void successfulHttpCallShouldProduceRecordOnlyOnSinkTopic() throws JsonProcessingException {
    Fixture fixture = loadFixture("collection", "collection_6");
    KaHPPMockServer.mockHttpResponse(fixture.getValue(), 200, "{\"new_value\":\"beautiful\"}");
    sendFixture(TOPIC_SOURCE, fixture);

    ConsumerRecord<String, String> recordSink =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    HttpRecordAssert.assertThat(recordSink).isEqualTo("{\"new_value\":\"beautiful\"}");
    HttpRecordAssert.assertThat(recordSink).hasHeaderWithStatus("SUCCESS");

    assertThatThrownBy(
            () ->
                KafkaTestUtils.getSingleRecord(
                    errorTopicConsumer, TOPIC_ERROR, KAFKA_CONSUMER_TIMEOUT_SHORT))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void erroredHttpCallShouldProduceRecordOnlyOnErrorTopic() throws JsonProcessingException {
    Fixture fixture = loadFixture("collection", "collection_6");
    KaHPPMockServer.mockHttpResponse(fixture.getValue(), 500);
    sendFixture(TOPIC_SOURCE, fixture);

    ConsumerRecord<String, String> recordOnError =
        KafkaTestUtils.getSingleRecord(errorTopicConsumer, TOPIC_ERROR);
    HttpRecordAssert.assertThat(recordOnError).isEqualTo(fixture.getValue());
    HttpRecordAssert.assertThat(recordOnError).hasHeaderWithStatus("ERROR_UNKNOWN");

    assertThatThrownBy(
            () ->
                KafkaTestUtils.getSingleRecord(
                    sinkTopicConsumer, TOPIC_SINK, KAFKA_CONSUMER_TIMEOUT_SHORT))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void htmlHttpResponseShouldGoToErrorTopic() {
    Fixture fixture = loadFixture("collection", "collection_6");
    KaHPPMockServer.mockHttpResponse(
        fixture.getValue(),
        200,
        "\n"
            + "<br />\n"
            + "Fatal error: Uncaught Error: Call to a member function error() on null in /error.php:1\n"
            + "Stack trace:\n"
            + "#0 {main}\n"
            + "  thrown in /error.php on line 1");
    sendFixture(TOPIC_SOURCE, fixture);

    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(errorTopicConsumer, TOPIC_ERROR);
    HttpRecordAssert.assertThat(record).hasHeaderWithStatus("ERROR_UNKNOWN");
  }

  @Configuration
  static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected Map<String, String> getTopics() {
      return Map.of(
          "source", AbstractKaHPPTest.TOPIC_SOURCE,
          "error", AbstractKaHPPTest.TOPIC_ERROR,
          "sink", AbstractKaHPPTest.TOPIC_SINK);
    }

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      var responseHandlerConfig = Map.of("type", "RECORD_UPDATE", "jmesPath", "value");

      StepConfiguration<OkOrProduceError> okOrProduceErrorStepConfiguration =
          new StepConfiguration<>(
              OkOrProduceError.class,
              "doAnAPICall",
              Map.of(
                  "api",
                  "defaultApi",
                  "path",
                  "enrich",
                  "responseHandler",
                  responseHandlerConfig,
                  ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC,
                  "error"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(okOrProduceErrorStepConfiguration, produceToTopicStep);
    }
  }
}
