package dev.vox.platform.kahpp.configuration.http.client.exception;

import dev.vox.platform.kahpp.configuration.http.client.Request;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import java.io.Serial;

public final class ServerException extends BadResponseException {

  @Serial private static final long serialVersionUID = 5195562719325929452L;

  private static final String MESSAGE = "Received server error response";

  public ServerException(final Request request, final Response response) {
    super(MESSAGE, request, response);
  }
}
