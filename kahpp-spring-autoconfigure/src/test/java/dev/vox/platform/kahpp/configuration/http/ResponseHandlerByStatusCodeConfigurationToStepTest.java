package dev.vox.platform.kahpp.configuration.http;

import static dev.vox.platform.kahpp.configuration.http.ResponseHandlerConfig.ResponseHandlerType.RECORD_UPDATE;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.filter.FilterValue;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

class ResponseHandlerByStatusCodeConfigurationToStepTest {

  @Test
  public void testConfigureWithoutResponseHandlers() {
    var handler = new ResponseHandlerByStatusCodeConfigurationToStep();

    var sourceStepConfig = stepConfig(emptyMap());
    var outputStepConfig = handler.configure(sourceStepConfig, configBuilder());
    assertThat(outputStepConfig).isSameAs(sourceStepConfig);
  }

  @Test
  public void testConfigureWithResponseHandler() {
    var handler = new ResponseHandlerByStatusCodeConfigurationToStep();
    Map<String, Object> responseHandlerConfig = Map.of("type", RECORD_UPDATE.toString());
    Map<String, Map<String, ?>> responseHandlers =
        Map.of(
            "0",
            Map.of(
                HttpCall.RESPONSE_HANDLER_CONFIG,
                responseHandlerConfig,
                "statusCodeStart",
                200,
                "statusCodeInclusiveEnd",
                299));
    var stepConfig = stepConfig(Map.of("responseHandlers", responseHandlers));
    StepConfiguration<HandleByStatusCode> config = handler.configure(stepConfig, configBuilder());
    assertThat(config).isNotNull();
  }

  StepConfiguration<HandleByStatusCode> stepConfig(Map<String, ?> configMap) {
    return new StepConfiguration<>(HandleByStatusCode.class, "step", configMap);
  }

  Instance.ConfigBuilder configBuilder() {
    var httpStep = new StepConfiguration<>(FilterValue.class, "", Map.of());
    return new Instance.ConfigBuilder(
        "test",
        "stepValidation",
        1,
        Map.of("source", "abc-topic"),
        new KafkaProperties.Streams(),
        Map.of(),
        List.of(httpStep));
  }
}
