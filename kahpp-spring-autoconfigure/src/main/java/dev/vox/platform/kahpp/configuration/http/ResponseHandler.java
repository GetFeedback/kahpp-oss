package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.http.client.Response;

public interface ResponseHandler {
  ResponseHandler RECORD_TERMINATE = response -> () -> false;
  ResponseHandler RECORD_FORWARD_AS_IS = response -> () -> true;

  RecordAction handle(Response response) throws ResponseHandlerException;
}
