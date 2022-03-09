package dev.vox.platform.kahpp.unit.processor;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.CLOCK_FIXED;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import dev.vox.platform.kahpp.configuration.IdempotentStep;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.conditional.ConditionFactory;
import dev.vox.platform.kahpp.configuration.conditional.Conditional;
import dev.vox.platform.kahpp.configuration.topic.Produce;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

class StepProcessorTest {

  private static final String STEP_NAME = "step";
  private static final String CHILD_STEP_NONE = "none";

  private transient MeterRegistry meterRegistry;
  private transient MockProcessorContext context;
  private transient MockClock mockClock;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockClock = new MockClock();
    meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, mockClock);
    context = new MockProcessorContext();
    context.setTimestamp(mockClock.wallTime());
    Instance instance =
        new Instance(
            new Instance.ConfigBuilder(
                "group",
                "name",
                null,
                Map.of(),
                new KafkaProperties.Streams(),
                Map.of(),
                List.of()),
            new StepBuilder(List.of()));
    InstanceRuntime.init(instance.getConfig(), CLOCK_FIXED);
    meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
  }

  @AfterEach
  void after() {
    InstanceRuntime.close();
  }

  @Test
  void forwardCountShouldIncrementAccordingToRecordOutcome() {
    new SimpleStepProcessor(meterRegistry, context).process(null, BooleanNode.valueOf(true));
    assertCountForward(1, true);
    assertCountForward(0, false);

    new SimpleStepProcessor(meterRegistry, context).process(null, BooleanNode.valueOf(false));
    assertCountForward(1, true);
    assertCountForward(1, false);
  }

  private void assertCountForward(int expected, boolean forwarded) {
    assertThat(
            meterRegistry
                .get("kahppMetricPrefix.forward")
                .tag(STEP_NAME, "SimpleStep")
                .tag("step_name", STEP_NAME)
                .tag("forwarded", Boolean.toString(forwarded))
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(expected);
  }

  private static class SimpleStepProcessor extends StepProcessor<SimpleStepProcessor.SimpleStep> {

    public SimpleStepProcessor(MeterRegistry meterRegistry, ProcessorContext context) {
      super(new SimpleStep(), new ChildStep("void"), meterRegistry);

      init(context);
    }

    @Override
    public void process(KaHPPRecord record) {
      if (record.getValue().asBoolean()) {
        forwardToNextStep(record);
      }
    }

    private static class SimpleStep implements Step {
      @Override
      public String getName() {
        return STEP_NAME;
      }
    }
  }

  @Test
  void produceCountShouldIncrementWhenForwardToProduceStepIsCalled() {
    new SimpleProduceStepProcessor(meterRegistry, context).process(null, null);
    assertCountProduce(1);

    new SimpleProduceStepProcessor(meterRegistry, context).process(null, null);
    assertCountProduce(2);
  }

  private void assertCountProduce(int expected) {
    assertThat(
            meterRegistry
                .get("kahppMetricPrefix.produce")
                .tag(STEP_NAME, "SimpleProduceStep")
                .tag("step_name", STEP_NAME)
                .tag("topic", "mock")
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(expected);
  }

  private static class SimpleProduceStepProcessor
      extends StepProcessor<SimpleProduceStepProcessor.SimpleProduceStep> {
    public SimpleProduceStepProcessor(MeterRegistry meterRegistry, ProcessorContext context) {
      super(new SimpleProduceStep(), new ChildStep("void"), meterRegistry);

      init(context);
    }

    @Override
    public void process(KaHPPRecord record) {
      forwardToSink(record, step());
    }

    private static class SimpleProduceStep implements Step, Produce {
      private final transient TopicIdentifier topic = new TopicIdentifier("mock");

      @Override
      public String getName() {
        return STEP_NAME;
      }

      @Override
      public Set<TopicIdentifier> eligibleSinkTopics() {
        return Set.of(this.topic);
      }

      @Override
      public TopicIdentifier produceToSink(KaHPPRecord record) {
        return this.topic;
      }
    }
  }

  @Test
  void conditionalSkipCountShouldIncrementWhenSkipsAStep() {
    new SimpleConditionalStepProcessor(meterRegistry, context)
        .process(null, BooleanNode.getFalse());
    assertCountConditional(1);

    new SimpleConditionalStepProcessor(meterRegistry, context).process(null, BooleanNode.getTrue());
    assertCountConditional(1);
  }

  @Test
  void shouldForwardWhenConditionIsNotSatisfied() {
    new SimpleConditionalStepProcessor(meterRegistry, context)
        .process(null, BooleanNode.getFalse());

    assertThat(context.forwarded().size()).isEqualTo(1);
    assertThat(context.forwarded().iterator().next().childName()).isEqualTo(CHILD_STEP_NONE);
  }

  @Test
  void shouldTerminateWhenConditionIsSatisfied() {
    new SimpleConditionalStepProcessor(meterRegistry, context).process(null, BooleanNode.getTrue());

    assertThat(context.forwarded().size()).isEqualTo(0);
  }

  private void assertCountConditional(int expected) {
    assertThat(
            meterRegistry
                .get("kahppMetricPrefix.conditional_skip")
                .tag(STEP_NAME, "SimpleConditional")
                .tag("step_name", STEP_NAME)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(expected);
  }

  private static class SimpleConditionalStepProcessor
      extends StepProcessor<SimpleConditionalStepProcessor.SimpleConditional> {

    public SimpleConditionalStepProcessor(MeterRegistry meterRegistry, ProcessorContext context) {
      super(new SimpleConditional(), new ChildStep(CHILD_STEP_NONE), meterRegistry);

      init(context);
    }

    @Override
    public void process(KaHPPRecord record) {
      // do nothing
    }

    private static class SimpleConditional implements Conditional {
      @Override
      public @NotNull Condition condition() {
        return new ConditionFactory(new JacksonRuntime()).createCondition("value==`true`");
      }

      @Override
      public String getName() {
        return STEP_NAME;
      }
    }
  }

  private static class SimpleIdempotentStepProcessor
      extends StepProcessor<SimpleIdempotentStepProcessor.SimpleIdempotentStep> {

    public SimpleIdempotentStepProcessor(
        MeterRegistry meterRegistry, MockProcessorContext mockProcessorContext) {
      super(new SimpleIdempotentStep(), new ChildStep("void"), meterRegistry);

      init(mockProcessorContext);
    }

    @Override
    public void process(KaHPPRecord record) {
      if (record.getValue().asBoolean()) {
        forwardToNextStep(record);
      }
    }

    private static class SimpleIdempotentStep implements IdempotentStep {
      @Override
      public String getName() {
        return STEP_NAME;
      }
    }
  }

  @Test
  void shouldSetStepAsDoneIfIdempotent() throws JsonProcessingException {
    final MockProcessorContext mockProcessorContext = new MockProcessorContext();
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add("foo", objectMapper.writeValueAsBytes("bar"));
    mockProcessorContext.setHeaders(recordHeaders);
    mockProcessorContext.setTimestamp(mockClock.wallTime());

    SimpleIdempotentStepProcessor simpleIdempotentStepProcessor =
        new SimpleIdempotentStepProcessor(meterRegistry, mockProcessorContext);
    simpleIdempotentStepProcessor.process(null, BooleanNode.getTrue());
    assertThat(
            simpleIdempotentStepProcessor
                .step()
                .isAlreadyDone(simpleIdempotentStepProcessor.context()))
        .isTrue();
  }
}
