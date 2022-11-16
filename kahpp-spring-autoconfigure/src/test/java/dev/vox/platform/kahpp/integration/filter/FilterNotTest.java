package dev.vox.platform.kahpp.integration.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.filter.FilterValue;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.integration.filter.FilterNotTest.KafkaStreamsFilterNotTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = KafkaStreamsFilterNotTest.class)
class FilterNotTest extends AbstractKaHPPTest {

  @Autowired private transient MeterRegistry meterRegistry;

  @Test
  void testFilterNotEnabledProducts() {
    processCollectionFixture("collection_5", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(1.0, false);

    processCollectionFixture("collection_6", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(1.0, true);
    assertKahppFilterEvaluationMetric(1.0, false);

    processCollectionFixture("collection_1", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(2.0, true);
    assertKahppFilterEvaluationMetric(1.0, false);

    processCollectionFixture("collection_3", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(3.0, true);
    assertKahppFilterEvaluationMetric(1.0, false);

    processCollectionFixture("collection_2", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(3.0, true);
    assertKahppFilterEvaluationMetric(2.0, false);
  }

  @Test
  void trueEvaluationWithFilterNotDoesNotContinue() {
    Fixture fixture = loadFixture("collection", "collection_5");
    sendFixture(TOPIC_SOURCE, fixture);
    assertThatThrownBy(
            () ->
                KafkaTestUtils.getSingleRecord(
                    sinkTopicConsumer, TOPIC_SINK, KAFKA_CONSUMER_TIMEOUT_SHORT))
        .isInstanceOf(IllegalStateException.class);
  }

  private void assertKahppFilterEvaluationMetric(double actual, boolean forwarded) {
    assertThat(actual)
        .isEqualTo(
            meterRegistry
                .get("kahpp.filter")
                .tag("step", "FilterValue")
                .tag("step_name", "keepUnsupportedProducts")
                .tag("forwarded", Boolean.toString(forwarded))
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum());
  }

  @SuppressWarnings("PMD.TestClassWithoutTestCases")
  @Configuration
  static class KafkaStreamsFilterNotTest extends KafkaStreamsTest {
    @Override
    protected Map<String, String> getTopics() {
      return Map.of("source", AbstractKaHPPTest.TOPIC_SOURCE, "sink", AbstractKaHPPTest.TOPIC_SINK);
    }

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<FilterValue> getUnsupportedProducts =
          new StepConfiguration<>(
              FilterValue.class,
              "keepUnsupportedProducts",
              Map.of("jmesPath", SUPPORTED_PRODUCTS_JMES_FILTER, "filterNot", "true"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(getUnsupportedProducts, produceToTopicStep);
    }
  }
}
