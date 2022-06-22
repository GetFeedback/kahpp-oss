package dev.vox.platform.kahpp.configuration.http;

import static dev.vox.platform.kahpp.configuration.http.ResponseHandlerConfig.ResponseHandlerType.RECORD_ROUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResponseHandlerBuilderTest {

  @Test
  void testConstructor() {
    assertThatCode(ResponseHandlerBuilder::new).doesNotThrowAnyException();

    Map<String, String> topics = new LinkedHashMap<>();
    topics.put("0", "topic1");
    topics.put("1", "topic2");
    Map<String, Object> config = new HashMap<>();
    config.put(ResponseHandlerBuilder.TYPE, RECORD_ROUTE.toString());
    config.put(ResponseHandlerBuilder.JMES_PATH, "type");
    config.put(ResponseHandlerBuilder.TOPICS, topics);
    ResponseHandler responseHandler = ResponseHandlerBuilder.build(config);
    assertThat(responseHandler).isNotNull();
    assertThat(responseHandler).isInstanceOf(ResponseHandlerRecordRoute.class);
  }
}
