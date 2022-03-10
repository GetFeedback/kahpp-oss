package dev.vox.platform.kahpp.configuration.predicate;

import dev.vox.platform.kahpp.configuration.topic.Produce;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.step.StepMetricUtils;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateStepProcessor extends StepProcessor<PredicateBranch> {

  private static final String METRIC_TAG_FORWARDED = "forwarded";

  private static final Logger LOGGER = LoggerFactory.getLogger(PredicateStepProcessor.class);

  private final transient JacksonRuntime jacksonRuntime;
  private final transient Counter forwardedCounter;
  private final transient Counter terminatedCounter;

  public PredicateStepProcessor(
      PredicateBranch step,
      ChildStep child,
      MeterRegistry meterRegistry,
      JacksonRuntime jacksonRuntime) {
    super(step, child, meterRegistry);
    this.jacksonRuntime = jacksonRuntime;

    final String metricNameCounter = StepMetricUtils.formatMetricName(step());
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

  @Override
  public void process(KaHPPRecord record) {
    boolean evaluation = step().test(jacksonRuntime, record);

    if (evaluation ^ !step().isRight()) {
      forwardedCounter.increment();

      forwardToNextStep(record);

      return;
    }

    if (step() instanceof Produce) {
      Produce p = (Produce) step();

      forwardToSink(record, p);

      return;
    }

    terminatedCounter.increment();

    LOGGER.debug(
        "{}: Record is being terminated due to `{}` as {}",
        step().getTypedName(),
        step().getJmesPath(),
        step().isRight() ? "right" : "left");
  }
}
