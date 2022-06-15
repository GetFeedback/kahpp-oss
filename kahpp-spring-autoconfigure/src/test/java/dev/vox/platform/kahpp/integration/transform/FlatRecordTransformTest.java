package dev.vox.platform.kahpp.integration.transform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.transform.SplitValueTransform;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = FlatRecordTransformTest.KStreamsTest.class)
class FlatRecordTransformTest extends AbstractKaHPPTest {

  @Test
  void recordIsSplitIntoMultipleWithValuesFoundAtJmesPath() {
    sendFixture(TOPIC_SOURCE, "splitting", "containing_array");

    ConsumerRecords<String, String> sinkRecords =
        KafkaTestUtils.getRecords(sinkTopicConsumer, 1000);
    ConsumerRecords<String, String> errorRecords =
        KafkaTestUtils.getRecords(errorTopicConsumer, 1000);

    assertThat(sinkRecords.count()).isEqualTo(3);
    assertThat(errorRecords.count()).isEqualTo(0);

    AtomicInteger index = new AtomicInteger();
    sinkRecords
        .records(TOPIC_SINK)
        .iterator()
        .forEachRemaining(
            (record) -> {
              index.getAndIncrement();
              assertThat(record.key()).isEqualTo("{\"id\":\"keep-this-id\"}");
              assertThat(record.value())
                  .isEqualTo("{\"property\":\"value " + index.toString() + "\"}");
            });
  }

  @Configuration
  public static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<SplitValueTransform> splitRecordStep =
          new StepConfiguration<>(
              SplitValueTransform.class, "splitIt", Map.of("jmesPath", "value.pathToArray"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(splitRecordStep, produceToTopicStep);
    }
  }
}
