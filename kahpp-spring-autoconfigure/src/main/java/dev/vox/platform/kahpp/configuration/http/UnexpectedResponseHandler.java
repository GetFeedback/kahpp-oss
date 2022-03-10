package dev.vox.platform.kahpp.configuration.http;

import com.usabilla.retryableapiclient.RequestException;
import dev.vox.platform.kahpp.configuration.RecordAction;

/**
 * Contract for ResponseHandler types which are also able to Handle a RequestException, since the
 * response might be present within this Exception it might decide to use.
 */
public interface UnexpectedResponseHandler extends ResponseHandler {
  RecordAction handle(RequestException requestException)
      throws ResponseHandlerException, RequestException;
}
