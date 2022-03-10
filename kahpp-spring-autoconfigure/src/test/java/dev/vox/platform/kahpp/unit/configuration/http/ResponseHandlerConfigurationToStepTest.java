package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.ResponseHandler;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerConfigurationToStep;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerRecordUpdate;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerConfigurationToStepTest {

  private final ResponseHandlerConfigurationToStep configToStep =
      new ResponseHandlerConfigurationToStep();

  @Test
  public void shouldHaveDefaultResponseHandler() {
    var configuration = new StepConfiguration<>(HttpCall.class, "test", Map.of());

    StepConfiguration<HttpCall> configure = configToStep.configure(configuration, null);
    var newConfig = configure.getConfig();

    assertThat(newConfig).containsKey("responseHandler");
    var responseHandler = newConfig.get("responseHandler");
    assertThat(responseHandler).isExactlyInstanceOf(ResponseHandlerRecordUpdate.class);
    assertThat(responseHandler).isSameAs(ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE);
  }

  @Test
  public void canInjectAResponseHandler() {
    var configuration =
        new StepConfiguration<>(
            HttpCall.class,
            "test",
            Map.of("responseHandler", Map.of("type", "RECORD_FORWARD_AS_IS")));

    StepConfiguration<HttpCall> configure = configToStep.configure(configuration, null);
    var newConfig = configure.getConfig();

    assertThat(newConfig).containsKey("responseHandler");
    var responseHandler = newConfig.get("responseHandler");
    assertThat(responseHandler).isInstanceOf(ResponseHandler.class);
    assertThat(responseHandler).isSameAs(ResponseHandler.RECORD_FORWARD_AS_IS);
  }
}
