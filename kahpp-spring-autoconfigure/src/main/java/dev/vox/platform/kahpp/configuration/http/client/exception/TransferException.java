package dev.vox.platform.kahpp.configuration.http.client.exception;

import dev.vox.platform.kahpp.configuration.http.client.Request;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import java.io.IOException;
import java.io.Serial;
import java.util.Optional;

public final class TransferException extends RequestException {

  @Serial private static final long serialVersionUID = -1194093232207688694L;

  private final Request request;

  public TransferException(final IOException cause, final Request request) {
    super(cause);
    this.request = request;
  }

  @Override
  public Optional<Request> getRequest() {
    return Optional.of(request);
  }

  @Override
  public Optional<Response> getResponse() {
    return Optional.empty();
  }
}
