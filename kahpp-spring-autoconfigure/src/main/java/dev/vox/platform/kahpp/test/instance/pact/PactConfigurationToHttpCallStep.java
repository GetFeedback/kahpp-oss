package dev.vox.platform.kahpp.test.instance.pact;

import com.usabilla.retryableapiclient.ApiClient;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.http.HttpClient;
import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance;
import java.util.HashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
@Order(500) // Ensure we can modify the Step and inject the Pact Server URI
public class PactConfigurationToHttpCallStep implements ConfigurationToStep<HttpCall> {

  private final transient PactMockServiceRegistry pactMockServiceRegistry;

  public PactConfigurationToHttpCallStep(PactMockServiceRegistry pactMockServiceRegistry) {
    this.pactMockServiceRegistry = pactMockServiceRegistry;
  }

  @Override
  public StepConfiguration<HttpCall> configure(
      StepConfiguration<HttpCall> stepConfiguration, Instance.ConfigBuilder configBuilder) {
    ApiClient apiClient = null;
    HashMap<String, Object> config = new HashMap<>(stepConfiguration.getConfig());

    if (config.containsKey("api")) {
      String apiIdentifier = config.get("api").toString();
      pactMockServiceRegistry.createPactMockerService(apiIdentifier);

      String basePath = pactMockServiceRegistry.getPactMockService(apiIdentifier).getServiceUri();

      HttpClient currentHttpClient = configBuilder.getApis().get(apiIdentifier);
      HttpClient updateHttpClient = new HttpClient(basePath, currentHttpClient.getOptions());
      apiClient = updateHttpClient.buildApiClient();
    }

    config.put("apiClient", apiClient);

    return stepConfiguration.newConfig(config);
  }

  @Override
  public Class<HttpCall> supportsType() {
    return HttpCall.class;
  }
}
