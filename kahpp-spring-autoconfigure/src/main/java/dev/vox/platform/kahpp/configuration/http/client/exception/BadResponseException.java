package dev.vox.platform.kahpp.configuration.http.client.exception;

import dev.vox.platform.kahpp.configuration.http.client.Request;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import java.io.Serial;
import java.util.Optional;

public abstract class BadResponseException extends RequestException {

  @Serial private static final long serialVersionUID = -244362035541033852L;

  private final Request request;
  private final Response response;

  BadResponseException(final String message, final Request request, final Response response) {
    super(message);
    this.request = request;
    this.response = response;
  }

  @Override
  public Optional<Request> getRequest() {
    return Optional.of(request);
  }

  @Override
  public Optional<Response> getResponse() {
    return Optional.of(response);
  }
}
