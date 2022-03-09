package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(210)
public final class ResponseHandlerConfigurationToStep implements ConfigurationToStep<HttpCall> {

  @SuppressWarnings("unchecked")
  @Override
  public StepConfiguration<HttpCall> configure(
      StepConfiguration<HttpCall> stepConfiguration, ConfigBuilder configBuilder) {
    final HashMap<String, Object> config = new HashMap<>(stepConfiguration.getConfig());

    ResponseHandler responseHandler;

    if (!config.containsKey(HttpCall.RESPONSE_HANDLER_CONFIG)) {
      // fixme: No more default responseHandler..
      responseHandler = ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE;
    } else {
      var handlerConfig = (Map<String, String>) config.get(HttpCall.RESPONSE_HANDLER_CONFIG);
      responseHandler = ResponseHandlerBuilder.build(handlerConfig);
    }

    config.put(HttpCall.RESPONSE_HANDLER_CONFIG, responseHandler);

    return stepConfiguration.newConfig(config);
  }

  @Override
  public Class<HttpCall> supportsType() {
    return HttpCall.class;
  }
}
