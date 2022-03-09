package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.util.Range;
import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(210)
public final class ResponseHandlerByStatusCodeConfigurationToStep
    implements ConfigurationToStep<HandleByStatusCode> {

  @Override
  @SuppressWarnings("unchecked")
  public StepConfiguration<HandleByStatusCode> configure(
      StepConfiguration<HandleByStatusCode> stepConfiguration, ConfigBuilder configBuilder) {
    final HashMap<String, Object> config = new HashMap<>(stepConfiguration.getConfig());

    Map<String, Map<String, ?>> responseHandlers =
        (Map<String, Map<String, ?>>) config.get(HandleByStatusCode.RESPONSE_HANDLERS);

    if (responseHandlers == null) return stepConfiguration;

    Map<Range, ResponseHandler> handlersByStatusCode = new HashMap<>();

    responseHandlers.forEach(
        (s, stringMap) -> {
          Range httpStatusCodeRange =
              new Range(
                  (int) stringMap.get("statusCodeStart"),
                  (int) stringMap.get("statusCodeInclusiveEnd"));

          var responseHandlerConfig =
              (Map<String, String>) stringMap.get(HttpCall.RESPONSE_HANDLER_CONFIG);

          ResponseHandler responseHandler = ResponseHandlerBuilder.build(responseHandlerConfig);
          handlersByStatusCode.put(httpStatusCodeRange, responseHandler);
        });

    config.put(HandleByStatusCode.RESPONSE_HANDLERS, handlersByStatusCode);

    return stepConfiguration.newConfig(config);
  }

  @Override
  public Class<HandleByStatusCode> supportsType() {
    return HandleByStatusCode.class;
  }
}
