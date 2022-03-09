package dev.vox.platform.kahpp.unit.configuration.topic;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.topic.Route;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

class RouteTest extends ConstraintViolationTestAbstract {
  private static final String TOPIC_NAME = "my-topic-in-kafka";
  private static final TopicIdentifier TOPIC_IDENTIFIER = new TopicIdentifier(TOPIC_NAME);
  private static final String JMES_PATH = "path == 'value'";
  private static final JacksonRuntime RUNTIME = new JacksonRuntime();
  private static final Expression<JsonNode> PREDICATE = RUNTIME.compile(JMES_PATH);
  private static final Route ROUTE = new Route(PREDICATE, TOPIC_IDENTIFIER);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void canBeConstructed() {
    Set<ConstraintViolation<Route>> violations = validator.validate(ROUTE);

    assertThat(violations).hasSize(0);
    assertThat(ROUTE.getTopic()).isEqualTo(TOPIC_IDENTIFIER);
  }

  @Test
  void cannotBeConstructedWithNullValues() {
    Set<ConstraintViolation<Route>> violations = validator.validate(new Route(null, null));

    assertThat(violations).hasSize(2);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of("predicate", List.of("must not be null"), "topic", List.of("must not be null")));
  }

  @Test
  void testReturnsFalseWhenSearchResultIsNotABoolean() {
    Expression<JsonNode> predicate = RUNTIME.compile("{id: value.id}");
    Route route = new Route(predicate, TOPIC_IDENTIFIER);

    ObjectNode node = OBJECT_MAPPER.createObjectNode();
    node.set("value", OBJECT_MAPPER.createObjectNode().put("id", "identifier"));

    assertThat(predicate.search(node).isBoolean()).isFalse();
    assertThat(route.test(node)).isFalse();
  }

  @ParameterizedTest
  @MethodSource("provideValuesThatAreEqual")
  void testEquals(Object comparisonValue) {
    assertThat(ROUTE).isEqualTo(comparisonValue);
    assertThat(ROUTE.hashCode()).isEqualTo(comparisonValue.hashCode());
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideValuesThatAreEqual() {
    return Stream.of(
        ROUTE,
        new Route(PREDICATE, TOPIC_IDENTIFIER),
        new Route(RUNTIME.compile(JMES_PATH), new TopicIdentifier(TOPIC_NAME)));
  }

  @ParameterizedTest
  @MethodSource("provideValuesThatAreNotEqual")
  @NullSource
  void testNotEquals(Object comparisonValue) {
    assertThat(ROUTE).isNotEqualTo(comparisonValue);
  }

  @SuppressWarnings("unused")
  private static Stream<Object> provideValuesThatAreNotEqual() {
    return Stream.of(
        new Object(),
        new Route(PREDICATE, new TopicIdentifier("other-topic")),
        new Route(RUNTIME.compile("path != 'value'"), TOPIC_IDENTIFIER),
        new Route(RUNTIME.compile("path != 'value'"), new TopicIdentifier("other-topic")));
  }
}
