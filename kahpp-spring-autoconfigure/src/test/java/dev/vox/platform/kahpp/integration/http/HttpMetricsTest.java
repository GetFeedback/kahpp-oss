package dev.vox.platform.kahpp.integration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.integration.KaHPPMockServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = HttpForwardAndProduceErrorTest.KStreamsTest.class)
class HttpMetricsTest extends AbstractKaHPPTest {

  private static final String RESPONSE_BODY_VALUE = "{}";
  public static final String HTTP_CALL_PATH = "/enrich";

  @Autowired private transient MeterRegistry meterRegistry;

  @BeforeAll
  static void setupMockServer() {
    KaHPPMockServer.initServer();
  }

  @AfterAll
  static void stopMockServer() {
    KaHPPMockServer.closeServer();
  }

  @Test
  void httpMetricsWithSuccessfulTagAreCreated() {
    Fixture fixture = loadFixture("collection", "collection_6");

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, fixture.getValue(), 200, RESPONSE_BODY_VALUE);
    sendFixture(TOPIC_SOURCE, fixture);
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    assertStepMetricsCount(1.0, true);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, fixture.getValue(), 500);
    sendFixture(TOPIC_SOURCE, fixture);
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    assertStepMetricsCount(1.0, false);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, fixture.getValue(), 200, RESPONSE_BODY_VALUE);
    sendFixture(TOPIC_SOURCE, fixture);
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    assertStepMetricsCount(2.0, true);
    assertStepMetricsCount(1.0, false);
  }

  @Test
  void httpDurationMetricsWithSuccessfulTagAreCreated() {
    Fixture fixture = loadFixture("collection", "collection_6");

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, fixture.getValue(), 200, RESPONSE_BODY_VALUE);
    sendFixture(TOPIC_SOURCE, fixture);
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertTimeMetric(true, 1L);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, fixture.getValue(), 429, RESPONSE_BODY_VALUE);
    sendFixture(TOPIC_SOURCE, fixture);
    KafkaTestUtils.getSingleRecord(errorTopicConsumer, TOPIC_ERROR);

    assertTimeMetric(false, 1L);

    KaHPPMockServer.mockHttpResponse(HTTP_CALL_PATH, fixture.getValue(), 200, RESPONSE_BODY_VALUE);
    sendFixture(TOPIC_SOURCE, fixture);
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertTimeMetric(true, 2L);
    assertTimeMetric(false, 1L);
  }

  private void assertTimeMetric(boolean successful, long expected) {
    assertThat(
            meterRegistry
                .get("kahpp.http.duration")
                .tag("step", "OkOrProduceError")
                .tag("step_name", "doAnAPICall")
                .tag("successful", Boolean.toString(successful))
                .timer()
                .count())
        .isEqualTo(expected);
  }

  private void assertStepMetricsCount(double expected, boolean successful) {
    assertThat(
            meterRegistry
                .get("kahpp.http.status")
                .tag("step", "OkOrProduceError")
                .tag("step_name", "doAnAPICall")
                .tag("successful", Boolean.toString(successful))
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(expected);
  }
}
