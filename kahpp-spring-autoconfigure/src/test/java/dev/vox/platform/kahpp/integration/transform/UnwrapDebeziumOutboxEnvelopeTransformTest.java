package dev.vox.platform.kahpp.integration.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.configuration.transform.UnwrapDebeziumOutboxEnvelopeTransform;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = UnwrapDebeziumOutboxEnvelopeTransformTest.KStreamsTest.class)
public class UnwrapDebeziumOutboxEnvelopeTransformTest extends AbstractKaHPPTest {

  @Test
  void transformWithSuccess() throws JsonProcessingException {
    sendFixture(TOPIC_SOURCE, "collection", "outbox_not_wrapped");

    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    assertThat(record.value())
        .isEqualTo(
            "{\"id\":\"bc6a5194-0807-4df7-a5f7-d6ad9f816361\",\"email\":\"test@kahpp.dev\"}");
    assertThat(record.headers().headers("vox.outbox.event"))
        .contains(
            new RecordHeader("vox.outbox.event", OBJECT_MAPPER.writeValueAsBytes("cake-test")));
    assertThat(record.headers().headers("vox.outbox.aggregateId"))
        .contains(
            new RecordHeader(
                "vox.outbox.aggregateId",
                OBJECT_MAPPER.writeValueAsBytes("bc6a5194-0807-4df7-a5f7-d6ad9f816361")));
    assertThat(record.headers().headers("vox.outbox.aggregateType"))
        .contains(
            new RecordHeader("vox.outbox.aggregateType", OBJECT_MAPPER.writeValueAsBytes("cake")));
    assertThat(record.headers().headers("vox.outbox.payloadType"))
        .contains(
            new RecordHeader("vox.outbox.payloadType", OBJECT_MAPPER.writeValueAsBytes("cake")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"outbox_not_stringified", "outbox_wrong_payload"})
  void transformWithError(String directory) {
    sendFixture(TOPIC_SOURCE, "collection", directory);

    assertThatThrownBy(() -> KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK))
        .isInstanceOf(IllegalStateException.class);
  }

  @Configuration
  public static class KStreamsTest extends KafkaStreamsTest {

    @Override
    protected List<StepConfiguration<? extends Step>> getSteps() {
      final StepConfiguration<UnwrapDebeziumOutboxEnvelopeTransform> unwrapDebeziumStep =
          new StepConfiguration<>(
              UnwrapDebeziumOutboxEnvelopeTransform.class,
              "unwrapDebezium",
              Map.of("copyEnvelopeFieldsToHeaders", "true"));

      final StepConfiguration<ProduceToTopic> produceToTopicStep =
          new StepConfiguration<>(
              ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

      return List.of(unwrapDebeziumStep, produceToTopicStep);
    }
  }
}
