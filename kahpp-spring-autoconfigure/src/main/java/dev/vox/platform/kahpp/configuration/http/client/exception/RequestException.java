package dev.vox.platform.kahpp.configuration.http.client.exception;

import dev.vox.platform.kahpp.configuration.http.client.Request;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import java.io.Serial;
import java.util.Optional;

public abstract class RequestException extends Exception {

  @Serial private static final long serialVersionUID = -3092516536737940816L;

  RequestException(final String message) {
    super(message);
  }

  RequestException(final Exception cause) {
    super(cause);
  }

  public abstract Optional<Request> getRequest();

  public abstract Optional<Response> getResponse();
}
