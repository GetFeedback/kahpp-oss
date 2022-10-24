package dev.vox.platform.kahpp.integration.meter;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.meter.CounterMeter;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.test.utils.KafkaTestUtils;

final class MeterTest extends AbstractKaHPPTest {

  private static final String METRIC_COLLECTION_PER_CHANNEL = "kahpp.collectionPerChannel";

  @Autowired private transient MeterRegistry meterRegistry;

  @Test
  void testMeterRegistryForChannelCount() {
    sendCollectionFeedbackItemFixture("collection_2");

    final ConsumerRecord<String, String> firstRecord =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(firstRecord).isNotNull();
    assertThat(
            meterRegistry
                .get(METRIC_COLLECTION_PER_CHANNEL)
                .tag("step", "CounterMeter")
                .tag("step_name", "collectionPerChannel")
                .tag("channel", "collection_2")
                .counter()
                .count())
        .isEqualTo(1.0);

    sendCollectionFeedbackItemFixture("collection_5");
    final ConsumerRecord<String, String> secondRecord =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(secondRecord).isNotNull();
    assertThat(
            meterRegistry
                .get(METRIC_COLLECTION_PER_CHANNEL)
                .tag("step", "CounterMeter")
                .tag("step_name", "collectionPerChannel")
                .tag("channel", "collection_5")
                .counter()
                .count())
        .isEqualTo(1.0);

    assertThat(
            meterRegistry.get(METRIC_COLLECTION_PER_CHANNEL).counters().stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(2.0);
  }

  @Test
  void testJsonWithoutChannelShouldBeUnknown() {
    processCollectionFixture("collection_1", TOPIC_SINK);

    assertThat(
            meterRegistry
                .get(METRIC_COLLECTION_PER_CHANNEL)
                .tag("step", "CounterMeter")
                .tag("step_name", "collectionPerChannel")
                .tag("channel", CounterMeter.JMESPATH_METRIC_NON_MATCH_DEFAULT_VALUE)
                .counter()
                .count())
        .isEqualTo(1.0);
  }
}
