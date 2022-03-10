package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.RecordActionRoute;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.HttpCallStepProcessor;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.step.ChildStep;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.vavr.control.Either;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

public class HttpCallStepProcessorRecordActionTest {

  private static final ObjectMapper MAPPER =
      JsonMapper.builder().addModules(new Jdk8Module(), new JavaTimeModule()).build();

  private static final String STEP_NAME = "HttpCallMock";
  private transient MeterRegistry meterRegistry;
  private transient HttpCallStepProcessor httpCallStepProcessor;
  private final transient Set<TopicEntry.TopicIdentifier> topics =
      Set.of(new TopicEntry.TopicIdentifier("topic1"), new TopicEntry.TopicIdentifier("topic2"));

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

    meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
    String fake = "fake";
    httpCallStepProcessor =
        new HttpCallStepProcessor(
            new HttpCallMock(), new ChildStep(fake), meterRegistry, new JacksonRuntime());

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
    httpCallStepProcessor.process(
        KaHPPRecord.build(MAPPER.createObjectNode(), MAPPER.createObjectNode(), 1584352842123L));
    assertThat(getProduceCountOnTopic("topic1")).isEqualTo(Double.valueOf(1.0));
    assertThat(getProduceCountOnTopic("topic2")).isEqualTo(Double.valueOf(1.0));
  }

  private Double getProduceCountOnTopic(String topic) {
    return meterRegistry
        .get("kahppMetricPrefix.produce")
        .tag("step_name", STEP_NAME)
        .tag("topic", topic)
        .counter()
        .count();
  }

  private class HttpCallMock implements HttpCall {

    @Override
    public Either<Throwable, RecordAction> call(KaHPPRecord record) {
      return Either.right(new RecordActionTest());
    }

    @Override
    public boolean shouldForwardRecordOnError() {
      return false;
    }

    @Override
    public String getName() {
      return STEP_NAME;
    }
  }

  private class RecordActionTest implements RecordActionRoute {
    @Override
    public boolean shouldForward() {
      return false;
    }

    @Override
    public Set<TopicEntry.TopicIdentifier> routes() {
      return topics;
    }
  }
}
