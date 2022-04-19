package dev.vox.platform.kahpp.configuration.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import org.springframework.util.StringUtils;

public final class ResponseHandlerRecordUpdate implements ResponseHandler {

  private static final String REPLACE_VALUE = "value";
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().registerModule(new Jdk8Module());

  public static final ResponseHandler RECORD_VALUE_REPLACE =
      new ResponseHandlerRecordUpdate(REPLACE_VALUE);

  private final transient String jmesPath;

  public ResponseHandlerRecordUpdate(String jmesPath) {
    this.jmesPath = jmesPath;
  }

  @Override
  public TransformRecord handle(Response response) throws ResponseHandlerException {
    return TransformRecord.replacePath(parseResponseBody(response), jmesPath);
  }

  /**
   * This PMD rule is being ignored here as when a JsonProcessingException is serialized, there is
   * an infinite loop due to its internal serialization, to work around this problem the message has
   * been extracted and a new Exception has been raised.
   */
  @SuppressWarnings("PMD.PreserveStackTrace")
  private static JsonNode parseResponseBody(Response response) throws ResponseHandlerException {
    var body = response.getBody().orElse(null);
    if (!StringUtils.hasText(body)) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readTree(body);
    } catch (JsonProcessingException e) {
      throw new ResponseHandlerException(e.getMessage());
    }
  }
}
