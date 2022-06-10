package dev.vox.platform.kahpp.integration.transform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.transform.CopyFieldRecordTransform;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = CopyFieldRecordTransformTest.KStreamsTest.class)
class CopyFieldRecordTransformTest extends AbstractKaHPPTest {

  @Test
  void transformWithSuccess() {
    sendFixture(TOPIC_SOURCE, "collection", "simple_record");

    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(record.value()).isEqualTo("{\"foo\":\"bar\",\"fooCopy\":\"bar\"}");
  }

  @Configuration
  public static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<CopyFieldRecordTransform> copyFieldStep =
          new StepConfiguration<>(
              CopyFieldRecordTransform.class,
              "testCopyField",
              Map.of("from", "value.foo", "to", "value.fooCopy"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(copyFieldStep, produceToTopicStep);
    }
  }
}
