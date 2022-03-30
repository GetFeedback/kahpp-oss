package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.http.client.exception.RequestException;

/**
 * Contract for ResponseHandler types which are also able to Handle a RequestException, since the
 * response might be present within this Exception it might decide to use.
 */
public interface UnexpectedResponseHandler extends ResponseHandler {
  RecordAction handle(RequestException requestException)
      throws ResponseHandlerException, RequestException;
}
