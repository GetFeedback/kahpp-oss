package dev.vox.platform.kahpp.configuration.http;

import com.usabilla.retryableapiclient.Response;
import dev.vox.platform.kahpp.configuration.RecordAction;

public interface ResponseHandler {
  ResponseHandler RECORD_TERMINATE = response -> () -> false;
  ResponseHandler RECORD_FORWARD_AS_IS = response -> () -> true;

  RecordAction handle(Response response) throws ResponseHandlerException;
}
