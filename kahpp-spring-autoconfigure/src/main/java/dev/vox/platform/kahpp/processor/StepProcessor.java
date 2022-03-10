package dev.vox.platform.kahpp.processor;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.configuration.IdempotentStep;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.conditional.Conditional;
import dev.vox.platform.kahpp.configuration.topic.Produce;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.step.StepMetricUtils;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.To;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StepProcessor<T extends Step> implements Processor<JsonNode, JsonNode> {

  private static final String METRIC_NAME_FORWARD = "forward";
  private static final String METRIC_NAME_PRODUCE = "produce";
  private static final String METRIC_NAME_CONDITIONAL = "conditional_skip";
  private static final String METRIC_TAG_FORWARDED = "forwarded";

  private static final Logger LOGGER = LoggerFactory.getLogger(StepProcessor.class);

  private final transient T currentStep;
  private final transient ChildStep child;
  private final transient MeterRegistry meterRegistry;

  private final transient Counter forwardedCounter;
  private final transient Counter terminatedCounter;

  private transient ProcessorContext processorContext;
  private transient Boolean wasRecordForwarded = false;

  protected StepProcessor(T currentStep, ChildStep child, MeterRegistry meterRegistry) {
    this.currentStep = currentStep;
    this.child = child;
    this.meterRegistry = meterRegistry;

    final String metricNameCounter = StepMetricUtils.formatMetricName(METRIC_NAME_FORWARD);
    final List<Tag> stepTags = StepMetricUtils.getStepTags(step());
    forwardedCounter =
        Counter.builder(metricNameCounter)
            .tags(stepTags)
            .tag(METRIC_TAG_FORWARDED, "true")
            .register(meterRegistry);
    terminatedCounter =
        Counter.builder(metricNameCounter)
            .tags(stepTags)
            .tag(METRIC_TAG_FORWARDED, "false")
            .register(meterRegistry);
  }

  public abstract void process(KaHPPRecord record);

  @Override
  public final void init(ProcessorContext context) {
    this.processorContext = context;
  }

  @Override
  public final void process(JsonNode key, JsonNode value) {
    LOGGER.debug("{}: Starting step", step().getTypedName());

    final KaHPPRecord record =
        KaHPPRecord.build(key, value, context().timestamp(), context().headers());
    if (step() instanceof Conditional) {
      final Condition condition = ((Conditional) step()).condition();
      if (!condition.test(record)) {
        LOGGER.debug("{}: forwarding due to `{}`", step().getTypedName(), condition.toString());

        Counter.builder(StepMetricUtils.formatMetricName(METRIC_NAME_CONDITIONAL))
            .tags(StepMetricUtils.getStepTags(step()))
            .register(meterRegistry)
            .increment();

        forwardToNextStep(record);
        return;
      }
    }

    // If a Step is IdempotentStep AND it's been already processed, skip it.
    if (step() instanceof IdempotentStep) {
      final IdempotentStep step = (IdempotentStep) step();
      if (step.isAlreadyDone(context())) {
        LOGGER.debug(
            "{}: forwarding due to step being idempotent and already processed",
            step().getTypedName());
        forwardToNextStep(record);
        return;
      }
      LOGGER.debug(
          "{}: idempotent check didn't pass, continuing to processing the step",
          step().getTypedName());
    }

    process(record);

    if (!wasRecordForwarded) {
      LOGGER.debug("{}: Record terminated", step().getTypedName());
      terminatedCounter.increment();
    }
  }

  @Override
  public void close() {}

  public final ProcessorContext context() {
    return processorContext;
  }

  public final T step() {
    return currentStep;
  }

  public final void forwardToNextStep(KaHPPRecord record) {
    wasRecordForwarded = true;

    LOGGER.debug("{}: Record forwarded to step `{}`", step().getTypedName(), child.getName());
    forwardedCounter.increment();

    forward(record, child.to());
  }

  public final void forwardToSink(KaHPPRecord record, Produce step) {
    final TopicEntry.TopicIdentifier topic = step.produceToSink(record);

    LOGGER.debug("{}: Record forwarded to sink `{}`", step.getTypedName(), topic.asString());

    forwardToTopic(record, topic);
  }

  public final void forwardToTopic(KaHPPRecord record, TopicEntry.TopicIdentifier topic) {
    Counter.builder(StepMetricUtils.formatMetricName(METRIC_NAME_PRODUCE))
        .tags(StepMetricUtils.getStepTags(step()))
        .tag("topic", topic.asString())
        .register(meterRegistry)
        .increment();

    forward(record, topic.to());
  }

  private void forward(KaHPPRecord record, To child) {
    // If the Step is IdempotentStep, mark it as done, as at this moment we know for sure internal
    // Step exception can't the thrown anymore.
    if (step() instanceof IdempotentStep) {
      final IdempotentStep step = (IdempotentStep) step();
      if (!step.isAlreadyDone(context())) {
        step.setAsDone(context());
      }
    }

    context().forward(record.getKey(), record.getValue(), child);
  }
}
