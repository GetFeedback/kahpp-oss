package dev.vox.platform.kahpp.configuration.http.client.exception;

import dev.vox.platform.kahpp.configuration.http.client.Request;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import java.io.Serial;

public final class ClientException extends BadResponseException {

  @Serial private static final long serialVersionUID = -2883911608585494281L;

  private static final String MESSAGE = "Received client error response";

  public ClientException(final Request request, final Response response) {
    super(MESSAGE, request, response);
  }
}
