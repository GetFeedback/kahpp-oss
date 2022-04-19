package dev.vox.platform.kahpp.test.instance.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.configuration.http.client.exception.RequestException;
import dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration;
import dev.vox.platform.kahpp.test.instance.test.KaHPPTestScenario;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

@Profile("test")
@Component
public class PactMockServiceRegistry {
  private static final int MOCK_SERVICE_PORT = 80;
  private static final String MOCK_SERVICE_PACT_FILES_LOCATION = "/tmp/pacts";
  private static final Map<String, PactMockService> apiMockServiceMap = new HashMap<>();

  public void createPactMockerService(String apiIdentifier) {
    if (apiMockServiceMap.containsKey(apiIdentifier)) {
      return;
    }
    PactMockService mockService = new PactMockService();
    apiMockServiceMap.put(apiIdentifier, mockService);
  }

  public PactMockService getPactMockService(String apiIdentifier) {
    return apiMockServiceMap.get(apiIdentifier);
  }

  public void generateAllPacts(String consumerIdentifier, Path copyPactToDirectory) {
    apiMockServiceMap.forEach(
        (apiIdentifier, pactMockService) -> {
          pactMockService.generatePact(consumerIdentifier, apiIdentifier);
          pactMockService.copyPactTo(
              String.format("%s-%s", consumerIdentifier, apiIdentifier), copyPactToDirectory);
        });
  }

  /**
   * Per Provider API:
   *
   * <p>- Clears previous Pact interactions added by previous scenarios
   *
   * <p>- Adds new interactions
   */
  public void setupInteractions(KaHPPTestScenario scenario) {
    scenario
        .getExpectedApiInteractions()
        .forEach(
            (apiIdentifier, interactions) -> {
              PactMockService pactMockService = this.getPactMockService(apiIdentifier);
              pactMockService.clearInteractions();
              pactMockService.addInteraction(interactions);
            });
  }

  public void verifyInteractions(KaHPPTestScenario scenario) {
    scenario
        .getExpectedApiInteractions()
        .keySet()
        .forEach(
            apiIdentifier -> {
              PactMockService pactMockService = this.getPactMockService(apiIdentifier);
              pactMockService.verifyInteractions();
            });
  }

  protected static class PactMockService {
    private final transient Container container;
    private final transient ApiClient internalClient;

    @SuppressWarnings({"PMD.CloseResource", "PMD.AvoidUsingHardCodedIP"})
    public PactMockService() {
      GenericContainer container = new GenericContainer("pactfoundation/pact-cli:0.50.0.18");
      container
          .withCommand(
              "mock-service",
              "-p",
              String.valueOf(MOCK_SERVICE_PORT),
              "--host",
              "0.0.0.0",
              "--pact-dir",
              MOCK_SERVICE_PACT_FILES_LOCATION)
          .withExposedPorts(MOCK_SERVICE_PORT)
          .start();

      this.container = container;
      this.internalClient =
          new ApiClient.Builder(getServiceUri())
              .setRequestConfig(100, 300)
              .setHeaders(Map.of("X-Pact-Mock-Service", "true"))
              .build();
      try {
        // Poor implemented WaitStrategy
        internalClient.sendRequest("GET", "/");
      } catch (RequestException e) {
        throw new RuntimeException("Pact Service hasn't started", e);
      }
    }

    private void addInteraction(JsonNode interaction) throws RequestException {
      this.internalClient.sendRequest("POST", "/interactions", interaction.toPrettyString());
    }

    public void generatePact(String consumerIdentifier, String apiIdentifier) {
      capturePactApiCall(
          () -> {
            ObjectNode pactPayload = InstanceTestConfiguration.MAPPER.createObjectNode();
            pactPayload.set(
                "consumer",
                InstanceTestConfiguration.MAPPER
                    .createObjectNode()
                    .put("name", consumerIdentifier));
            pactPayload.set(
                "provider",
                InstanceTestConfiguration.MAPPER.createObjectNode().put("name", apiIdentifier));
            this.internalClient.sendRequest("POST", "/pact", pactPayload.toPrettyString());
          });
    }

    public final String getServiceUri() {
      return String.format(
          "http://%s:%s",
          container.getContainerIpAddress(), container.getMappedPort(MOCK_SERVICE_PORT));
    }

    public void clearInteractions() {
      capturePactApiCall(() -> this.internalClient.sendRequest("DELETE", "/interactions"));
    }

    public void copyPactTo(String pactName, Path destination) {
      Assertions.assertThatCode(
              () -> {
                String pactFile = String.format("%s.json", pactName);
                container.copyFileFromContainer(
                    String.format("%s/%s", MOCK_SERVICE_PACT_FILES_LOCATION, pactFile),
                    String.format("%s/%s", destination.toAbsolutePath().toString(), pactFile));
              })
          .doesNotThrowAnyException();
    }

    public void addInteraction(Map<String, JsonNode> interactions) {
      interactions
          .values()
          .forEach(interaction -> capturePactApiCall(() -> this.addInteraction(interaction)));
    }

    public void verifyInteractions() {
      capturePactApiCall(
          () -> this.internalClient.sendRequest("GET", "/interactions/verification"));
    }

    private static void capturePactApiCall(ThrowingCallable shouldRaiseThrowable) {
      RequestException exception =
          Assertions.catchThrowableOfType(shouldRaiseThrowable, RequestException.class);

      AtomicReference<String> responseJson = new AtomicReference<>("");
      AtomicReference<String> requestJson = new AtomicReference<>("");
      Assertions.assertThatCode(
              () -> {
                if (exception == null) {
                  return;
                }
                responseJson.set(
                    InstanceTestConfiguration.MAPPER.writeValueAsString(
                        exception.getResponse().orElseThrow()));
                requestJson.set(
                    InstanceTestConfiguration.MAPPER.writeValueAsString(
                        exception.getRequest().orElseThrow()));
              })
          .doesNotThrowAnyException();

      Assertions.assertThat(exception)
          .withFailMessage(
              "[Pact call failed because: %s, request was: %s]",
              responseJson.get(), requestJson.get())
          .doesNotThrowAnyException();
    }
  }
}
