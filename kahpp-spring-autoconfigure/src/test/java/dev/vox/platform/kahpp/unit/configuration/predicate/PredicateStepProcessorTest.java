package dev.vox.platform.kahpp.unit.configuration.predicate;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.predicate.PredicateOrProduceError;
import dev.vox.platform.kahpp.configuration.predicate.PredicateStepProcessor;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PredicateStepProcessorTest {
  private static final String ERROR_TOPIC_IDENTIFIER = "my-topic";
  private static final String STEP_CHILD_TEST = "void";

  private static final JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private static final TopicEntry.TopicIdentifier topicIdentifier =
      new TopicEntry.TopicIdentifier(ERROR_TOPIC_IDENTIFIER);

  private transient KaHPPRecord recordFooIsTrue;
  private transient KaHPPRecord recordFooIsFalse;

  @BeforeEach
  void setUp() {
    ObjectNode nodeTrue = MAPPER.createObjectNode();
    nodeTrue.put("foo", true);

    recordFooIsTrue = KaHPPRecord.build(null, nodeTrue, 1584352842123L);

    ObjectNode nodeFalse = MAPPER.createObjectNode();
    nodeFalse.put("foo", false);

    recordFooIsFalse = KaHPPRecord.build(null, nodeFalse, 1584352842123L);
  }

  @Test
  void rightPredicate() {
    final PredicateOrProduceError predicate =
        new PredicateOrProduceError(
            "test-leftPredicate",
            Map.of(
                "jmesPath", "value.foo == `true`",
                "either", "right",
                "topic", topicIdentifier));

    final PredicateStepProcessor predicateStepProcessor =
        new PredicateStepProcessor(
            predicate, new ChildStep(STEP_CHILD_TEST), new SimpleMeterRegistry(), jacksonRuntime);

    final MockProcessorContext context1 = new MockProcessorContext();
    predicateStepProcessor.init(context1);
    predicateStepProcessor.process(recordFooIsFalse);
    assertThat(context1.forwarded().size()).isEqualTo(1);
    assertThat(context1.forwarded().iterator().next().childName())
        .isEqualTo(ERROR_TOPIC_IDENTIFIER);

    final MockProcessorContext context2 = new MockProcessorContext();
    predicateStepProcessor.init(context2);
    predicateStepProcessor.process(recordFooIsTrue);
    assertThat(context2.forwarded().size()).isEqualTo(1);
    assertThat(context2.forwarded().iterator().next().childName()).isEqualTo(STEP_CHILD_TEST);
  }

  @Test
  void leftPredicate() {
    final PredicateOrProduceError predicate =
        new PredicateOrProduceError(
            "test-leftPredicate",
            Map.of(
                "jmesPath", "value.foo == `true`",
                "either", "left",
                "topic", topicIdentifier));

    final PredicateStepProcessor predicateStepProcessor =
        new PredicateStepProcessor(
            predicate, new ChildStep(STEP_CHILD_TEST), new SimpleMeterRegistry(), jacksonRuntime);

    final MockProcessorContext context1 = new MockProcessorContext();
    predicateStepProcessor.init(context1);
    predicateStepProcessor.process(recordFooIsFalse);
    assertThat(context1.forwarded().size()).isEqualTo(1);
    assertThat(context1.forwarded().iterator().next().childName()).isEqualTo(STEP_CHILD_TEST);

    final MockProcessorContext context2 = new MockProcessorContext();
    predicateStepProcessor.init(context2);
    predicateStepProcessor.process(recordFooIsTrue);
    assertThat(context2.forwarded().size()).isEqualTo(1);
    assertThat(context2.forwarded().iterator().next().childName())
        .isEqualTo(ERROR_TOPIC_IDENTIFIER);
  }
}
