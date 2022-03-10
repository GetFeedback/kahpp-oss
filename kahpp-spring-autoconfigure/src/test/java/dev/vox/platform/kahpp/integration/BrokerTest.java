package dev.vox.platform.kahpp.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.usabilla.healthcheck.springboot.kafka.Topics;
import dev.vox.platform.kahpp.KafkaStreams;
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
    Topics streamsTopics = kafkaStreams.getTopics();
    Set<String> brokerTopics = embeddedKafka.getTopics();

    assertThat(brokerTopics).hasSizeGreaterThan(0);
    assertThat(brokerTopics).contains(streamsTopics.all().toArray(new String[0]));
  }
}
