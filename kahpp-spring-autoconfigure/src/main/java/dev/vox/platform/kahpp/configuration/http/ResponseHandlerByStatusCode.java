package dev.vox.platform.kahpp.configuration.http;

import com.usabilla.retryableapiclient.RequestException;
import com.usabilla.retryableapiclient.Response;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.configuration.util.Range;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseHandlerByStatusCode implements UnexpectedResponseHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResponseHandlerByStatusCode.class);

  private final Map<Range, ResponseHandler> statusCodeHandlers;

  public ResponseHandlerByStatusCode(Map<Range, ResponseHandler> statusCodeHandlers) {
    this.statusCodeHandlers = Map.copyOf(statusCodeHandlers);
  }

  @Override
  public RecordAction handle(Response response) throws ResponseHandlerException {
    Optional<Map.Entry<Range, ResponseHandler>> first =
        statusCodeHandlers.entrySet().stream()
            .filter(e -> e.getKey().contains(response.getStatusCode()))
            .findFirst();

    if (first.isPresent()) {
      Map.Entry<Range, ResponseHandler> handlerEntry = first.get();
      LOGGER.debug(
          "{}: Matching record with handler `{}` due to status code `{}`",
          getClass().getSimpleName(),
          handlerEntry.getValue().getClass().getSimpleName(),
          response.getStatusCode());
      return handlerEntry.getValue().handle(response);
    }

    LOGGER.warn(
        "{}: No responseHandler matches due to status code `{}`",
        getClass().getSimpleName(),
        response.getStatusCode());
    throw new ResponseHandlerException(
        String.format(
            "No responseHandler matched with status code `%s`", response.getStatusCode()));
  }

  @Override
  public RecordAction handle(RequestException requestException)
      throws RequestException, ResponseHandlerException {
    Response response = requestException.getResponse().orElseThrow(() -> requestException);

    try {
      return this.handle(response);
    } catch (ResponseHandlerException e) {
      e.addSuppressed(requestException);
      throw e;
    }
  }

  public Set<TopicEntry.TopicIdentifier> getTopics() {
    return statusCodeHandlers.values().stream()
        .filter(c -> c instanceof ResponseHandlerRecordRoute)
        .map(c -> ((ResponseHandlerRecordRoute) c).getTopics())
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }
}
