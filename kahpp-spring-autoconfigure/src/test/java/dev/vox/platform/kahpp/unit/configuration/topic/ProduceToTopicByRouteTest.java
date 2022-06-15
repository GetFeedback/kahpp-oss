package dev.vox.platform.kahpp.unit.configuration.topic;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopicByRoute;
import dev.vox.platform.kahpp.configuration.topic.Route;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ProduceToTopicByRouteTest {
  private static final TopicIdentifier ERROR_TOPIC = new TopicIdentifier("error");
  private static final String STEP_NAME = "dynamicRouter";
  private final transient JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private ProduceToTopicByRoute router;

  @BeforeEach
  void setUp() {
    List<Route> routes =
        List.of(
            routeOf("value.routeTo == 'x'", "topic-x"),
            routeOf("value.routeTo == 'y'", "topic-y"),
            routeOf("value.routeTo == 'z'", "topic-z"));

    this.router =
        new ProduceToTopicByRoute(
            STEP_NAME, Map.of("name", "xyzRouter", "errorTopic", ERROR_TOPIC, "routes", routes));
  }

  @Test
  void getNameShouldReturnConfiguredName() {
    assertThat(router.getName()).isEqualTo(STEP_NAME);
  }

  @Test
  void shouldListAllTopics() {
    assertThat(router.eligibleSinkTopics()).size().isEqualTo(4);
    assertThat(router.eligibleSinkTopics())
        .containsExactly(
            new TopicIdentifier("topic-x"),
            new TopicIdentifier("topic-y"),
            new TopicIdentifier("topic-z"),
            new TopicIdentifier("error"));
  }

  @Test
  void shouldRouteToTopic() {
    assertThat(router.produceToSink(recordWithRoute("x")))
        .isEqualTo(new TopicIdentifier("topic-x"));
    assertThat(router.produceToSink(recordWithRoute("y")))
        .isEqualTo(new TopicIdentifier("topic-y"));
    assertThat(router.produceToSink(recordWithRoute("z")))
        .isEqualTo(new TopicIdentifier("topic-z"));
  }

  @Test
  void shouldRouteToFirstMatch() {
    List<Route> withTwoMatchingRoutes =
        List.of(
            routeOf("key.id == 'foo'", "topic-x"),
            routeOf("value.routeTo == 'x'", "topic-y"),
            routeOf("value.routeTo == 'z'", "topic-z"));

    final ProduceToTopicByRoute router =
        new ProduceToTopicByRoute(
            STEP_NAME,
            Map.of(
                "name", "xyzRouter", "errorTopic", ERROR_TOPIC, "routes", withTwoMatchingRoutes));

    assertThat(router.produceToSink(recordWithRoute("x")).asString()).isEqualTo("topic-x");
  }

  @Test
  void shouldRouteToErrorTopicWhenThereIsNoMatch() {
    assertThat(router.produceToSink(recordWithRoute("e"))).isEqualTo(new TopicIdentifier("error"));
  }

  private Route routeOf(String jmesPath, String topicIdentifier) {
    return new Route(this.jacksonRuntime.compile(jmesPath), new TopicIdentifier(topicIdentifier));
  }

  private static KaHPPRecord recordWithRoute(String route) {
    ObjectNode key = MAPPER.createObjectNode().put("id", "foo");
    ObjectNode value = MAPPER.createObjectNode().put("random", "stuff").put("routeTo", route);
    return KaHPPRecord.build(key, value, 1584352842123L);
  }
}
