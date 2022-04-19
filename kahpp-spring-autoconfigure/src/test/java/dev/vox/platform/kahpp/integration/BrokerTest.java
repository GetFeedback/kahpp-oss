package dev.vox.platform.kahpp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.KafkaStreams;
import dev.vox.platform.kahpp.configuration.topic.TopicsMap;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public final class BrokerTest extends AbstractKaHPPTest {

  @Autowired private transient KafkaStreams kafkaStreams;

  @Test
  public void testBrokerIsAvailable() {
    String brokerList = embeddedKafka.getBrokersAsString();
    assertThat(brokerList).isNotEmpty();
  }

  @Test
  public void topicsAreCorrect() {
    TopicsMap streamsTopics = kafkaStreams.getTopics();
    Set<String> brokerTopics = embeddedKafka.getTopics();

    assertThat(brokerTopics).hasSizeGreaterThan(0);
    assertThat(brokerTopics).contains(streamsTopics.all().toArray(new String[0]));
  }
}
