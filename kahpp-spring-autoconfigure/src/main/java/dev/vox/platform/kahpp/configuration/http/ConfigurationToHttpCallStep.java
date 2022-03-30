package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance.ConfigBuilder;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(200)
public class ConfigurationToHttpCallStep implements ConfigurationToStep<HttpCall> {

  private final transient Map<String, ApiClient> apiClientRegistry = new HashMap<>();

  @Override
  public StepConfiguration<HttpCall> configure(
      StepConfiguration<HttpCall> stepConfiguration, ConfigBuilder configBuilder) {
    HashMap<String, Object> config = new HashMap<>(stepConfiguration.getConfig());
    if (!config.containsKey("api")) {
      return stepConfiguration;
    }

    String api = config.get("api").toString();

    if (!apiClientRegistry.containsKey(api)) {
      HttpClient httpClient = configBuilder.getApis().get(api);
      if (httpClient == null) {
        return stepConfiguration;
      }
      apiClientRegistry.put(api, httpClient.buildApiClient());
    }

    config.put("apiClient", apiClientRegistry.get(api));

    return stepConfiguration.newConfig(config);
  }

  @Override
  public Class<HttpCall> supportsType() {
    return HttpCall.class;
  }
}
