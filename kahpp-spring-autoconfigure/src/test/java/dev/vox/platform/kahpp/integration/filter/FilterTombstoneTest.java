package dev.vox.platform.kahpp.integration.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.filter.FilterTombstone;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = FilterTombstoneTest.KStreamsTest.class)
class FilterTombstoneTest extends AbstractKaHPPTest {

  private final Fixture tombstoneFixture = new Fixture();

  @Autowired private transient MeterRegistry meterRegistry;

  @BeforeEach
  public void setUpTombstoneFixture() {
    tombstoneFixture.setKey("{\"id\":\"record-id\"}\n");
    tombstoneFixture.setValue(null);
  }

  @Test
  void testFilterTombstoneRecords() {
    sendFixture(TOPIC_SOURCE, tombstoneFixture);
    assertFilterTombstoneMetric(1.0, false);

    sendCollectionFeedbackItemFixture("collection_2");
    assertFilterTombstoneMetric(1.0, false);
    assertFilterTombstoneMetric(1.0, true);

    sendFixture(TOPIC_SOURCE, tombstoneFixture);
    assertFilterTombstoneMetric(2.0, false);
    assertFilterTombstoneMetric(1.0, true);
  }

  @Test
  void trueEvaluationForFilterResultsInNoRecordsProduced() {
    sendFixture(TOPIC_SOURCE, tombstoneFixture);

    assertThatThrownBy(
            () ->
                KafkaTestUtils.getSingleRecord(
                    sinkTopicConsumer, TOPIC_SINK, KAFKA_CONSUMER_TIMEOUT_SHORT))
        .as("no records should be produced to 'sink' topic ")
        .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(
            () ->
                KafkaTestUtils.getSingleRecord(
                    errorTopicConsumer, TOPIC_ERROR, KAFKA_CONSUMER_TIMEOUT_SHORT))
        .as("no records should be produced to 'error' topic ")
        .isInstanceOf(IllegalStateException.class);
  }

  private void assertFilterTombstoneMetric(double expected, boolean forwarded) {
    assertThat(
            meterRegistry
                .get("kahpp.filter")
                .tag("step", "FilterTombstone")
                .tag("step_name", "dropTombstoneRecords")
                .tag("forwarded", Boolean.toString(forwarded))
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(expected);
  }

  @SuppressWarnings("PMD.TestClassWithoutTestCases")
  @Configuration
  static class KStreamsTest extends KafkaStreamsTest {
    @Override
    protected Map<String, String> getTopics() {
      return Map.of("source", AbstractKaHPPTest.TOPIC_SOURCE, "sink", AbstractKaHPPTest.TOPIC_SINK);
    }

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<FilterTombstone> filterTombstone =
          new StepConfiguration<>(
              FilterTombstone.class, "dropTombstoneRecords", Map.of("filterNot", "true"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(filterTombstone, produceToTopicStep);
    }
  }
}
