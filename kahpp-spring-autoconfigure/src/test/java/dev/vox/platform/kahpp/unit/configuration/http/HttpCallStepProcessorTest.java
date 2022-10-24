package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.HttpCallStepProcessor;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vavr.control.Either;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

class HttpCallStepProcessorTest {

  private static final String HTTP_STEP_NAME = "httpDelayable";
  private static final String HTTP_STEP_CLASS = "DelayableHttpCall";

  private static final ObjectMapper MAPPER =
      JsonMapper.builder().addModules(new Jdk8Module(), new JavaTimeModule()).build();

  private transient MockClock clock;
  private transient HttpCallStepProcessor httpCallStepProcessor;
  private transient MeterRegistry meterRegistry;

  @BeforeEach
  public void setUp() {
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
    InstanceRuntime.init(instance.getConfig());

    clock = new MockClock();
    meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
    String fake = "fake";
    httpCallStepProcessor =
        new HttpCallStepProcessor(
            new DelayableHttpCall(), new ChildStep(fake), meterRegistry, new JacksonRuntime());

    final MockProcessorContext context = new MockProcessorContext();
    context.setHeaders(new RecordHeaders());
    httpCallStepProcessor.init(context);
  }

  @AfterEach
  public void tearsDown() {
    InstanceRuntime.close();
  }

  @Test
  void testTimerMetric() {
    httpCallStepProcessor.process(createRecord(200, true));
    assertThat(getTimerMetric(true).count()).isEqualTo(1L);
    assertThat(getTimerMetric(true).mean(TimeUnit.MILLISECONDS)).isEqualTo(200);
    assertThat(getTimerMetric(true).totalTime(TimeUnit.MILLISECONDS)).isEqualTo(200);

    httpCallStepProcessor.process(createRecord(200, false));
    assertThat(getTimerMetric(false).count()).isEqualTo(1L);
    assertThat(getTimerMetric(false).mean(TimeUnit.MILLISECONDS)).isEqualTo(200);
    assertThat(getTimerMetric(false).totalTime(TimeUnit.MILLISECONDS)).isEqualTo(200);

    httpCallStepProcessor.process(createRecord(400, true));
    assertThat(getTimerMetric(true).count()).isEqualTo(2L);
    assertThat(getTimerMetric(false).count()).isEqualTo(1L);
    assertThat(getTimerMetric(true).mean(TimeUnit.MILLISECONDS)).isEqualTo(300);
    assertThat(getTimerMetric(true).totalTime(TimeUnit.MILLISECONDS)).isEqualTo(600);
  }

  @Test
  void testStatusMetric() {
    httpCallStepProcessor.process(createRecord(0, true));
    assertCountStatus(1D, true);

    httpCallStepProcessor.process(createRecord(0, false));
    assertCountStatus(1D, false);

    httpCallStepProcessor.process(createRecord(0, true));
    assertCountStatus(2D, true);
    assertCountStatus(1D, false);
  }

  @Test
  void testDistributionSummaryMetric() {
    httpCallStepProcessor.process(createRecord(200, true));

    assertThat(getDistributionSummary().count()).isEqualTo(1L);
    assertThat(getDistributionSummary().totalAmount()).isEqualTo(200.0);
    assertThat(getDistributionSummary().max()).isEqualTo(200.0);

    assertDistributionSummaryBucket(200.0, 1.);

    httpCallStepProcessor.process(createRecord(400, true));

    assertThat(getDistributionSummary().count()).isEqualTo(2L);
    assertThat(getDistributionSummary().totalAmount()).isEqualTo(600.0);
    assertThat(getDistributionSummary().max()).isEqualTo(400.0);

    assertDistributionSummaryBucket(200.0, 1.);
    assertDistributionSummaryBucket(400.0, 2.);
  }

  @Test
  void distributionSummaryShouldHaveInfiniteBucket() {
    final CountAtBucket[] countAtBuckets =
        getDistributionSummary().takeSnapshot().histogramCounts();
    final CountAtBucket lastBucket = countAtBuckets[countAtBuckets.length - 1];

    assertThat(lastBucket.bucket()).isEqualTo(Double.POSITIVE_INFINITY);
  }

  private KaHPPRecord createRecord(int delayMillis, boolean success) {
    ObjectNode key = MAPPER.createObjectNode();
    key.put("id", "42");

    ObjectNode value = MAPPER.createObjectNode();
    value.put("delayMillis", delayMillis);
    value.put("success", success);

    return KaHPPRecord.build(key, value, 1584352842123L);
  }

  private Timer getTimerMetric(boolean successful) {
    return meterRegistry
        .get("kahpp.http.duration")
        .tag("step", HTTP_STEP_CLASS)
        .tag("step_name", HTTP_STEP_NAME)
        .tag("successful", Boolean.toString(successful))
        .timer();
  }

  private DistributionSummary getDistributionSummary() {
    return meterRegistry
        .get("kahpp.http.response_time_ms")
        .tag("step", HTTP_STEP_CLASS)
        .tag("step_name", HTTP_STEP_NAME)
        .tag("successful", Boolean.toString(true))
        .summary();
  }

  private void assertDistributionSummaryBucket(double requestTimeMillis, double expectedCount) {
    for (CountAtBucket countAtBucket : getDistributionSummary().takeSnapshot().histogramCounts()) {
      if (countAtBucket.bucket() >= requestTimeMillis) {
        assertThat(countAtBucket.count()).isEqualTo(expectedCount);
        return;
      }
    }

    fail("Summary did not match");
  }

  private void assertCountStatus(double expected, boolean successful) {
    assertThat(
            meterRegistry
                .get("kahpp.http.status")
                .tag("step", HTTP_STEP_CLASS)
                .tag("step_name", HTTP_STEP_NAME)
                .tag("successful", Boolean.toString(successful))
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum())
        .isEqualTo(expected);
  }

  private class DelayableHttpCall implements HttpCall {
    @Override
    public Either<Throwable, RecordAction> call(KaHPPRecord record) {
      clock.add(Duration.ofMillis(record.getValue().get("delayMillis").asLong()));

      if (!record.getValue().get("success").asBoolean()) {
        return Either.left(new Exception());
      }

      return Either.right(TransformRecord.noTransformation());
    }

    @Override
    public boolean shouldForwardRecordOnError() {
      return false;
    }

    @Override
    public String getName() {
      return HTTP_STEP_NAME;
    }
  }
}
