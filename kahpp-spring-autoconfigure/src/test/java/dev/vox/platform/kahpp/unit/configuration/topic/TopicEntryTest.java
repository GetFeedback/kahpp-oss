package dev.vox.platform.kahpp.unit.configuration.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import java.util.stream.Stream;
import org.apache.kafka.streams.processor.To;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

class TopicEntryTest {
  private static final String TOPIC_NAME = "my-topic-in-kafka";
  private static final TopicIdentifier TOPIC_IDENTIFIER = new TopicIdentifier(TOPIC_NAME);
  private final transient TopicEntry topicEntry = new TopicEntry("topic-id", TOPIC_NAME);

  @Test
  void generatesTo() {
    To toActual = topicEntry.getIdentifier().to();
    assertThat(toActual).isEqualTo(To.child("topic-id"));
  }

  @Test
  void isImmutable() {
    assertThatThrownBy(() -> topicEntry.setValue("new-topic-in-kafka"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @ParameterizedTest
  @MethodSource("provideValuesThatAreEqual")
  void shouldEqualTopicIdentifier(Object comparisonValue) {
    assertThat(TOPIC_IDENTIFIER).isEqualTo(comparisonValue);
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideValuesThatAreEqual() {
    return Stream.of(TOPIC_IDENTIFIER, new TopicIdentifier(TOPIC_NAME));
  }

  @ParameterizedTest
  @MethodSource("provideValuesThatAreNotEqual")
  @NullSource
  void shouldNotEqualTopicIdentifier(Object comparisonValue) {
    assertThat(TOPIC_IDENTIFIER).isNotEqualTo(comparisonValue);
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideValuesThatAreNotEqual() {
    return Stream.of(TOPIC_NAME, new TopicIdentifier("other-topic"));
  }
}
