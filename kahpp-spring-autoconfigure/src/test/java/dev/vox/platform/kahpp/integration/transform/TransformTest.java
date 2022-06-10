package dev.vox.platform.kahpp.integration.transform;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.test.utils.KafkaTestUtils;

class TransformTest extends AbstractKaHPPTest {
  @Test
  void transformNotWrappedItem() throws JsonProcessingException {
    sendCollectionFeedbackItemFixture("collection_5_not_wrapped");
    ConsumerRecord<String, String> record =
        KafkaTestUtils.getSingleRecord(sinkTopicConsumer, TOPIC_SINK);

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(record.value());

    assertThat(jsonNode.has("payload")).isTrue();
    assertThat(jsonNode.get("operation").asText()).isEqualTo("\"create_cake\"");
    assertThat(jsonNode.get("publication_date").asText()).isNotNull();
  }
}
