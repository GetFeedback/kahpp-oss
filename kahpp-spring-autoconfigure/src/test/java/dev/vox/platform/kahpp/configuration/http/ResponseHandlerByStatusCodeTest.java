package dev.vox.platform.kahpp.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.usabilla.retryableapiclient.RequestException;
import com.usabilla.retryableapiclient.Response;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.util.Range;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerByStatusCodeTest {

  @Mock ResponseHandler foundHandler;
  @Mock RequestException requestException;

  RecordAction foundRecordAction = () -> false;

  @BeforeEach
  void setUp() throws ResponseHandlerException {
    lenient().when(foundHandler.handle(any())).thenReturn(foundRecordAction);
  }

  @Test
  public void whenThereAreNoHandlersItDoesFallback() {
    Map<Range, ResponseHandler> statusCodeHandlers = new HashMap<>();
    Response response = buildResponse(Map.of(), 204, "");
    var handler = new ResponseHandlerByStatusCode(statusCodeHandlers);

    assertThatThrownBy(() -> handler.handle(response))
        .hasNoSuppressedExceptions()
        .hasMessageContaining("204");
  }

  @Test
  public void whenNoMatchesOccurItThrowsAnException() {
    var statusCodeHandlers = Map.of(new Range(200, 299), foundHandler);
    Response response = buildResponse(Map.of(), 404, "");
    var handler = new ResponseHandlerByStatusCode(statusCodeHandlers);
    assertThatThrownBy(() -> handler.handle(response))
        .hasNoSuppressedExceptions()
        .hasMessageContaining("404");
  }

  @Test
  public void itDoesHandleWhenRangeMatches() throws ResponseHandlerException {
    var statusCodeHandlers = Map.of(new Range(200, 299), foundHandler);

    Response response = buildResponse(Map.of(), 200, "");
    var handler = new ResponseHandlerByStatusCode(statusCodeHandlers);

    assertThat(handler.handle(response)).isEqualTo(foundRecordAction);
    Mockito.verify(foundHandler).handle(response);
  }

  @Test
  public void canHandleExceptionsWithResponse() {
    Response response = buildResponse(Map.of(), 500, "err");

    lenient().when(requestException.getResponse()).thenReturn(Optional.of(response));

    var handler = new ResponseHandlerByStatusCode(Map.of());

    assertThatThrownBy(() -> handler.handle(requestException))
        .hasSuppressedException(requestException)
        .isInstanceOf(ResponseHandlerException.class)
        .hasMessageContaining("500");
  }

  @Test
  public void rethrowsRequestExceptionWhenThereIsNoResponse() {
    lenient().when(requestException.getResponse()).thenReturn(Optional.empty());

    var handler = new ResponseHandlerByStatusCode(Map.of());
    Throwable throwable = catchThrowable(() -> handler.handle(requestException));
    assertThat(throwable).isSameAs(requestException);
  }

  private Response buildResponse(Map<String, String> headers, int statusCode, String body) {
    // fixme: Should the RetryableApiClient open its Response constructor?
    try {
      Constructor<Response> declaredConstructor =
          Response.class.getDeclaredConstructor(Map.class, int.class, String.class);
      declaredConstructor.setAccessible(true);

      return declaredConstructor.newInstance(headers, statusCode, body);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }
}
