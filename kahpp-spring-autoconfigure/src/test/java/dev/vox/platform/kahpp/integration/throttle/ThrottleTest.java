package dev.vox.platform.kahpp.integration.throttle;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.throttle.Throttle;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = ThrottleTest.KStreamsTest.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ThrottleTest extends AbstractKaHPPTest {
  @Test
  void recordIsNotMutated() {
    sendFixture(TOPIC_SOURCE, "collection", "simple_record");

    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(record.key()).isEqualTo("{\"name\":\"simple_record\"}");
    assertThat(record.value()).isEqualTo("{\"foo\":\"bar\"}");
  }

  @Test
  void recordsAreThrottled() {
    // The first record includes startup time which influences the rate limiter. Ignoring it for
    // this test.
    sendFixture(TOPIC_SOURCE, "collection", "simple_record");
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    long start = Instant.now().toEpochMilli();
    sendFixture(TOPIC_SOURCE, "collection", "simple_record");
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    sendFixture(TOPIC_SOURCE, "collection", "simple_record");
    KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);
    long end = Instant.now().toEpochMilli();

    assertThat(end - start).isGreaterThanOrEqualTo(1000);
  }

  @Configuration
  public static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<Throttle> throttleStep =
          new StepConfiguration<>(Throttle.class, "throttle", Map.of("recordsPerSecond", 1));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(throttleStep, produceToTopicStep);
    }
  }
}
