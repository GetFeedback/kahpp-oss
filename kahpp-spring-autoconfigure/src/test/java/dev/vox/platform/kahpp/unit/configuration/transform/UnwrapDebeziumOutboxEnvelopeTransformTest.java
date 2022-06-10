package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.CLOCK_FIXED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.transform.UnwrapDebeziumOutboxEnvelopeException;
import dev.vox.platform.kahpp.configuration.transform.UnwrapDebeziumOutboxEnvelopeTransform;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

class UnwrapDebeziumOutboxEnvelopeTransformTest {

  private static final String STEP_NAME = "unwrapDebeziumOutboxEnvelope";

  private static final JacksonRuntime JACKSON_RUNTIME = new JacksonRuntime();

  private transient UnwrapDebeziumOutboxEnvelopeTransform recordTransform;

  @BeforeEach
  void setUp() {
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
    InstanceRuntime.close();
    InstanceRuntime.init(instance.getConfig(), CLOCK_FIXED);

    recordTransform =
        new UnwrapDebeziumOutboxEnvelopeTransform(
            STEP_NAME, Map.of("copyEnvelopeFieldsToHeaders", true));
  }

  @AfterEach
  void after() {
    InstanceRuntime.close();
  }

  @Test
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  void shouldUnwrapOutbox() {
    MockProcessorContext mockProcessorContext = new MockProcessorContext();
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add("foo", "bar".getBytes(StandardCharsets.UTF_8));
    mockProcessorContext.setHeaders(recordHeaders);
    final TransformRecord transformation =
        recordTransform.transform(
            JACKSON_RUNTIME,
            mockProcessorContext,
            KaHPPRecord.build(
                NullNode.getInstance(),
                json(
                    "{\n"
                        + "  \"payload\": \"{\\\"id\\\": \\\"bc6a5194-0807-4df7-a5f7-d6ad9f816361\\\", \\\"email\\\": \\\"test@kahpp.dev\\\"}\",\n"
                        + "  \"type\": \"cake-test\",\n"
                        + "  \"aggregateId\": \"bc6a5194-0807-4df7-a5f7-d6ad9f816361\",\n"
                        + "  \"aggregateType\": \"cake\",\n"
                        + "  \"payloadType\": \"cake\"\n"
                        + "}\n"),
                0));

    assertThat(transformation.getMutations().size()).isEqualTo(6);
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("aggregateId", "key.id"));
    assertThat(transformation.getMutations())
        .contains(
            TransformRecord.JmesPathMutation.pair("aggregateId", "headers.vox.outbox.aggregateId"));
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("payload", "value"));
    assertThat(transformation.getMutations())
        .contains(TransformRecord.JmesPathMutation.pair("type", "headers.vox.outbox.event"));
    assertThat(transformation.getMutations())
        .contains(
            TransformRecord.JmesPathMutation.pair("payloadType", "headers.vox.outbox.payloadType"));
    assertThat(transformation.getMutations())
        .contains(
            TransformRecord.JmesPathMutation.pair(
                "aggregateType", "headers.vox.outbox.aggregateType"));
    assertThat(transformation.getMutations())
        .contains(
            TransformRecord.JmesPathMutation.pair("payloadType", "headers.vox.outbox.payloadType"));
  }

  @Test
  void shouldSkipUnwrapBecauseVersion2() {
    MockProcessorContext mockProcessorContext = new MockProcessorContext();
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add("vox.outbox.metaVersion", String.valueOf(2).getBytes(StandardCharsets.UTF_8));
    mockProcessorContext.setHeaders(recordHeaders);
    final TransformRecord transformation =
        recordTransform.transform(
            JACKSON_RUNTIME,
            mockProcessorContext,
            KaHPPRecord.build(NullNode.getInstance(), NullNode.getInstance(), 0));

    assertThat(transformation.getMutations().size()).isEqualTo(0);
  }

  @Test
  void shouldFailInvalidOutbox() {
    MockProcessorContext mockProcessorContext = new MockProcessorContext();
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add("foo", "bar".getBytes(StandardCharsets.UTF_8));
    mockProcessorContext.setHeaders(recordHeaders);

    assertThatThrownBy(
            () ->
                recordTransform.transform(
                    JACKSON_RUNTIME,
                    mockProcessorContext,
                    KaHPPRecord.build(
                        json("{\"key\":\"ccde5853\"}"), json("{\"foo\":\"bar\"}"), 0)))
        .isInstanceOf(UnwrapDebeziumOutboxEnvelopeException.class)
        .hasMessageContaining(
            "Could not use payload: expected an Outbox V1 Payload but got something else, key: {\"key\":\"ccde5853\"}");
  }

  @Test
  void shouldFailNotStringifiedJsonPayload() {
    MockProcessorContext mockProcessorContext = new MockProcessorContext();
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add("foo", "bar".getBytes(StandardCharsets.UTF_8));
    mockProcessorContext.setHeaders(recordHeaders);

    assertThatThrownBy(
            () ->
                recordTransform.transform(
                    JACKSON_RUNTIME,
                    mockProcessorContext,
                    KaHPPRecord.build(
                        NullNode.getInstance(),
                        json(
                            "{\n"
                                + "  \"payload\": {\"id\": \"bc6a5194-0807-4df7-a5f7-d6ad9f816361\", \"email\": \"test@kahpp.dev\"},\n"
                                + "  \"type\": \"cake-test\",\n"
                                + "  \"aggregateId\": \"bc6a5194-0807-4df7-a5f7-d6ad9f816361\",\n"
                                + "  \"aggregateType\": \"cake\",\n"
                                + "  \"payloadType\": \"cake\"\n"
                                + "}\n"),
                        0)))
        .isInstanceOf(UnwrapDebeziumOutboxEnvelopeException.class)
        .hasMessageContaining(
            "Could not extract payload: expected a stringified JSON but got OBJECT");
  }

  @Test
  void shouldFailParsing() {
    MockProcessorContext mockProcessorContext = new MockProcessorContext();
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add("foo", "bar".getBytes(StandardCharsets.UTF_8));
    mockProcessorContext.setHeaders(recordHeaders);

    JacksonRuntime jacksonRuntimeMocked = mock(JacksonRuntime.class);
    when(jacksonRuntimeMocked.parseString(any())).thenThrow(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                recordTransform.transform(
                    jacksonRuntimeMocked,
                    mockProcessorContext,
                    KaHPPRecord.build(
                        json("{\"key\":\"ccde5853\"}"),
                        json(
                            "{\n"
                                + "  \"payload\": \"{\\\"id\\\": \\\"bc6a5194-0807-4df7-a5f7-d6ad9f816361\\\", \\\"email\\\": \\\"test@kahpp.dev\\\"}\",\n"
                                + "  \"type\": \"cake-test\",\n"
                                + "  \"aggregateId\": \"bc6a5194-0807-4df7-a5f7-d6ad9f816361\",\n"
                                + "  \"aggregateType\": \"cake\",\n"
                                + "  \"payloadType\": \"cake\"\n"
                                + "}\n"),
                        0)))
        .isInstanceOf(UnwrapDebeziumOutboxEnvelopeException.class)
        .hasMessageContaining(
            "Could not parsing payload to JsonNode: got error when trying to parse {\"key\":\"ccde5853\"}");
  }

  private static JsonNode json(String singleQuotedJson) {
    AtomicReference<JsonNode> json = new AtomicReference<>();

    assertDoesNotThrow(
        () ->
            json.set(
                InstanceTestConfiguration.MAPPER.readTree(singleQuotedJson.replaceAll("'", "\""))));

    return json.get();
  }
}
