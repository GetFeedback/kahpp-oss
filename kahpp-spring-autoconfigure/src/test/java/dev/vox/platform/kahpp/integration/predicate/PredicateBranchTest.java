package dev.vox.platform.kahpp.integration.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.Fixture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;

@SpringBootTest(classes = KafkaStreamPredicateTest.class)
public class PredicateBranchTest extends AbstractKaHPPTest {

  @Test
  void shouldSendWebPassiveFeedbackToSinkTopic() throws JsonProcessingException {
    Fixture fixture = sendCollectionFeedbackItemFixture("collection_6");

    final ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(record.value()).toPrettyString())
        .isEqualTo(objectMapper.readTree(fixture.getValue()).toPrettyString());
  }

  @Test
  void shouldSendSdkPassiveFeedbackToErrorTopic() throws JsonProcessingException {
    Fixture fixture = sendCollectionFeedbackItemFixture("collection_3");

    final ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(errorTopicConsumer, TOPIC_ERROR);

    ObjectMapper objectMapper = new ObjectMapper();
    assertThat(objectMapper.readTree(record.value()).toPrettyString())
        .isEqualTo(objectMapper.readTree(fixture.getValue()).toPrettyString());
  }
}
