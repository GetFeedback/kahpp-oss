package dev.vox.platform.kahpp.configuration.http.client.exception;

import dev.vox.platform.kahpp.configuration.http.client.Request;
import dev.vox.platform.kahpp.configuration.http.client.Response;

public final class ServerException extends BadResponseException {

  private static final String MESSAGE = "Received server error response";

  public ServerException(final Request request, final Response response) {
    super(MESSAGE, request, response);
  }
}
