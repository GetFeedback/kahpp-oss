package dev.vox.platform.kahpp.integration.http;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.http.SimpleHttpCall;
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

@SpringBootTest(classes = HttpConditionalTest.KStreamsTest.class)
class HttpConditionalTest extends AbstractKaHPPTest {

  @BeforeAll
  static void setupMockServer() {
    KaHPPMockServer.initServer();
  }

  @AfterAll
  static void stopMockServer() {
    KaHPPMockServer.closeServer();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void shouldExecuteHttpCall() {
    Fixture fixture = loadFixture("conditional", "true");

    KaHPPMockServer.mockHttpResponse("/enrich", fixture.getValue(), 200, "{}");
    sendFixture(TOPIC_SOURCE, fixture);

    ConsumerRecord<String, String> recordSink =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    HttpRecordAssert.assertThat(recordSink).hasHeaderWithStatus("SUCCESS");
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void shouldNotExecuteHttpCall() {
    Fixture fixture = loadFixture("conditional", "false");
    sendFixture(TOPIC_SOURCE, fixture);

    ConsumerRecord<String, String> recordSink =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    HttpRecordAssert.assertThat(recordSink).hasNoHeaders();
  }

  @Configuration
  static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected Map<String, String> getTopics() {
      return Map.of(
          "source", AbstractKaHPPTest.TOPIC_SOURCE,
          "sink", AbstractKaHPPTest.TOPIC_SINK);
    }

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      var responseHandlerConfig = Map.of("type", "RECORD_UPDATE", "jmesPath", "value");

      StepConfiguration<SimpleHttpCall> simpleHttpCallStepConfiguration =
          new StepConfiguration<>(
              SimpleHttpCall.class,
              "doAnAPICall",
              Map.of(
                  "api",
                  "defaultApi",
                  "path",
                  "enrich",
                  "condition",
                  "value.condition==`true`",
                  "responseHandler",
                  responseHandlerConfig));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(simpleHttpCallStepConfiguration, produceToTopicStep);
    }
  }
}
