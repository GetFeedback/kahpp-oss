package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.conditional.Conditional;
import dev.vox.platform.kahpp.configuration.http.client.ApiClient;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.vavr.control.Either;
import io.vavr.control.Try;
import java.util.Locale;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public abstract class AbstractHttpCall implements HttpCall, Conditional {
  @NotBlank private final transient String name;

  /** todo: Custom validator that exposes the missing api reference */
  @NotNull(message = "Could not find apiClient, possibly missing entry in `kahpp.apis`")
  private final transient ApiClient apiClient;

  @Pattern(regexp = "POST|PUT|PATCH")
  private transient String method = "POST";

  @NotNull protected transient ResponseHandler responseHandler;

  @Pattern(regexp = "true|false")
  private transient String forwardRecordOnError = "false";

  @NotNull private transient Condition condition;

  @NotBlank private transient String path;

  protected AbstractHttpCall(String name, Map<String, ?> config) {
    this.name = name;
    if (config.containsKey("path")) {
      this.path = config.get("path").toString();
    }

    if (config.containsKey("method")) {
      this.method = config.get("method").toString().toUpperCase(Locale.ROOT);
    }

    if (config.containsKey(RESPONSE_HANDLER_CONFIG)) {
      this.responseHandler = (ResponseHandler) config.get(RESPONSE_HANDLER_CONFIG);
    }

    if (config.containsKey("forwardRecordOnError")) {
      this.forwardRecordOnError =
          config.get("forwardRecordOnError").toString().toLowerCase(Locale.ROOT);
    }

    if (config.containsKey(STEP_CONFIGURATION_CONDITION)) {
      this.condition = (Condition) config.get(STEP_CONFIGURATION_CONDITION);
    }

    this.apiClient = loadApiClient(config);
  }

  @Override
  public @NotNull Condition condition() {
    return this.condition;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Either<Throwable, RecordAction> call(KaHPPRecord record) {
    ResponseHandler responseHandler = this.responseHandler;

    return Try.of(() -> apiClient.sendRequest(method, path, record.getValue().toString()))
        .mapTry(responseHandler::handle)
        .toEither();
  }

  @Override
  public boolean shouldForwardRecordOnError() {
    return Boolean.parseBoolean(forwardRecordOnError);
  }

  private static ApiClient loadApiClient(Map<String, ?> config) {
    Object candidateApiClient = config.get("apiClient");
    if (candidateApiClient instanceof ApiClient) {
      return (ApiClient) candidateApiClient;
    }

    return null;
  }
}
