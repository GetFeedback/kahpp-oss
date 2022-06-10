package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.RecordActionRoute;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.http.ResponseHandler;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerException;
import dev.vox.platform.kahpp.configuration.http.ResponseHandlerRecordRoute;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseHandlerTest {
  Response response;

  @BeforeEach
  @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
  void setUp()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException,
          InstantiationException {
    // fixme: Should the RetryableApiClient open its Response constructor?
    Constructor<Response> declaredConstructor =
        Response.class.getDeclaredConstructor(Map.class, int.class, String.class);
    declaredConstructor.setAccessible(true);
    this.response = declaredConstructor.newInstance(Map.of(), 200, "");
  }

  @Test
  void recordTerminateShouldNotTransformOrForward() throws ResponseHandlerException {
    ResponseHandler handler = ResponseHandler.RECORD_TERMINATE;

    assertThat(handler).isNotInstanceOf(TransformRecord.class);
    RecordAction action = handler.handle(this.response);
    assertThat(action.shouldForward()).isFalse();
  }

  @Test
  void recordForwardAsIsShouldNotTransformOrForward() throws ResponseHandlerException {
    ResponseHandler handler = ResponseHandler.RECORD_FORWARD_AS_IS;

    assertThat(handler).isNotInstanceOf(TransformRecord.class);
    RecordAction action = handler.handle(this.response);
    assertThat(action.shouldForward()).isTrue();
  }

  @Test
  void recordRouteShouldBeRecordActionRoute() throws ResponseHandlerException {
    TopicEntry.TopicIdentifier topic = new TopicEntry.TopicIdentifier("topic");
    ResponseHandlerRecordRoute responseHandler =
        new ResponseHandlerRecordRoute(Collections.singleton(topic));
    RecordActionRoute handle = responseHandler.handle(this.response);
    assertThat(handle.shouldForward()).isFalse();
    assertThat(handle.routes()).contains(topic);
    assertThat(responseHandler.getTopics()).contains(topic);
  }
}
