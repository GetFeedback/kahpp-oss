package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.RecordActionRoute;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.TransformRecordApplier;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import dev.vox.platform.kahpp.configuration.http.client.exception.RequestException;
import dev.vox.platform.kahpp.configuration.topic.Produce;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.processor.StepProcessor;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.step.StepMetricUtils;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.vavr.control.Either;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpCallStepProcessor extends StepProcessor<HttpCall> {

  private static final String METRIC_TAG_SUCCESSFUL = "successful";
  private static final String METRIC_SUFFIX_TIMER = "duration";
  private static final String METRIC_SUFFIX_DS = "response_time_ms";
  private static final String METRIC_SUFFIX_COUNT = "status";

  private static final double[] METRIC_DS_SLA_RANGE_MS = {
    1,
    2,
    5,
    10,
    20,
    30,
    50,
    100,
    200,
    300,
    500,
    1_000,
    2_000,
    3_000,
    5_000,
    10_000,
    Double.POSITIVE_INFINITY
  };

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpCallStepProcessor.class);

  private final transient MeterRegistry meterRegistry;
  private final transient JacksonRuntime jacksonRuntime;

  private final transient DistributionSummary successfulResponseTimeMs;
  private final transient DistributionSummary unsuccessfulResponseTimeMs;
  private final transient Timer successfulResponseTimer;
  private final transient Timer unsuccessfulResponseTimer;
  private final transient Counter successfulResponseCounter;
  private final transient Counter unsuccessfulResponseCounter;

  public HttpCallStepProcessor(
      HttpCall httpStep,
      ChildStep child,
      MeterRegistry meterRegistry,
      JacksonRuntime jacksonRuntime) {
    super(httpStep, child, meterRegistry);

    this.meterRegistry = meterRegistry;
    this.jacksonRuntime = jacksonRuntime;

    final List<Tag> stepTags = StepMetricUtils.getStepTags(step());

    final String metricNameDS = StepMetricUtils.formatMetricName(step(), METRIC_SUFFIX_DS);
    successfulResponseTimeMs =
        DistributionSummary.builder(metricNameDS)
            .serviceLevelObjectives(METRIC_DS_SLA_RANGE_MS)
            .tags(stepTags)
            .tag(METRIC_TAG_SUCCESSFUL, "true")
            .register(meterRegistry);
    unsuccessfulResponseTimeMs =
        DistributionSummary.builder(metricNameDS)
            .serviceLevelObjectives(METRIC_DS_SLA_RANGE_MS)
            .tags(stepTags)
            .tag(METRIC_TAG_SUCCESSFUL, "false")
            .register(meterRegistry);

    final String metricNameTimer = StepMetricUtils.formatMetricName(step(), METRIC_SUFFIX_TIMER);
    successfulResponseTimer =
        Timer.builder(metricNameTimer)
            .tags(stepTags)
            .tag(METRIC_TAG_SUCCESSFUL, "true")
            .register(meterRegistry);
    unsuccessfulResponseTimer =
        Timer.builder(metricNameTimer)
            .tags(stepTags)
            .tag(METRIC_TAG_SUCCESSFUL, "false")
            .register(meterRegistry);

    final String metricNameCounter = StepMetricUtils.formatMetricName(step(), METRIC_SUFFIX_COUNT);
    successfulResponseCounter =
        Counter.builder(metricNameCounter)
            .tags(stepTags)
            .tag(METRIC_TAG_SUCCESSFUL, "true")
            .register(meterRegistry);
    unsuccessfulResponseCounter =
        Counter.builder(metricNameCounter)
            .tags(stepTags)
            .tag(METRIC_TAG_SUCCESSFUL, "false")
            .register(meterRegistry);
  }

  @Override
  public void process(KaHPPRecord sourceRecord) {
    final Timer.Sample sample = Timer.start(meterRegistry);

    Either<Throwable, RecordAction> recordAction = step().call(sourceRecord);

    if (recordAction.isLeft()) {
      final long nanoInterval = sample.stop(unsuccessfulResponseTimer);

      var error = recordAction.getLeft();
      context().headers().add(InstanceRuntime.HeaderHelper.forError(step(), error));

      unsuccessfulResponseTimeMs.record(Duration.ofNanos(nanoInterval).toMillis());
      unsuccessfulResponseCounter.increment();

      LOGGER.warn(
          "{}: HTTP call failed with: {}", step().getTypedName(), getStatusCodeContext(error));

      if (step() instanceof Produce) {
        forwardToSink(sourceRecord, (Produce) step());
      }

      if (step().shouldForwardRecordOnError()) {
        forwardToNextStep(sourceRecord);
        return;
      }

      return;
    }

    final long nanoInterval = sample.stop(successfulResponseTimer);

    context().headers().add(InstanceRuntime.HeaderHelper.forSuccess(step()));

    successfulResponseTimeMs.record(Duration.ofNanos(nanoInterval).toMillis());
    successfulResponseCounter.increment();

    RecordAction action = recordAction.get();

    KaHPPRecord sinkRecord = sourceRecord;
    if (action instanceof TransformRecord) {
      sinkRecord =
          TransformRecordApplier.apply(jacksonRuntime, sourceRecord, (TransformRecord) action);
    } else if (action instanceof RecordActionRoute) {
      Set<TopicEntry.TopicIdentifier> routes = ((RecordActionRoute) action).routes();
      for (TopicEntry.TopicIdentifier topic : routes) {
        forwardToTopic(sinkRecord, topic);
      }
    }

    if (action.shouldForward()) {
      forwardToNextStep(sinkRecord);
      return;
    }

    // fixme: forward to
    // dev.vox.platform.kahpp.KafkaStreams.TopologyBuilder.KAHPP_INTERNAL_PROCESSOR_FINALIZE ?
    LOGGER.debug("{}: Record is being terminated", step().getTypedName());
  }

  private String getStatusCodeContext(Throwable throwable) {
    if (throwable instanceof RequestException) {
      final RequestException requestException = (RequestException) throwable;

      if (requestException.getResponse().isPresent()) {
        final Response response = requestException.getResponse().get();

        return String.format("`%s` http status code", response.getStatusCode());
      }
    }

    return throwable.getMessage();
  }
}
