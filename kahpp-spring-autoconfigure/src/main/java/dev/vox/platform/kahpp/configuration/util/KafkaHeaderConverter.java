package dev.vox.platform.kahpp.configuration.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaHeaderConverter {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHeaderConverter.class);

  private final ObjectMapper objectMapper;

  public KafkaHeaderConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper.copy();
  }

  public JsonNode convert(Headers recordHeaders) {
    if (recordHeaders == null) {
      return objectMapper.createArrayNode();
    }
    List<Map<String, JsonNode>> headers = new ArrayList<>();
    for (Header header : recordHeaders) {

      try {
        JsonNode value;
        value = objectMapper.readTree(header.value());
        headers.add(Map.of(header.key(), value));
      } catch (IOException e) {
        LOGGER.debug(
            String.format(
                "Error converting value of Header '%s' to JsonNode, skip it", header.key()));
      }
    }
    return objectMapper.valueToTree(headers);
  }

  public RecordHeaders convert(JsonNode jsonNode) {
    if (jsonNode == null || jsonNode.isNull()) {
      return new RecordHeaders();
    }
    List<Map<String, Object>> map = objectMapper.convertValue(jsonNode, new TypeReference<>() {});
    return convert(map);
  }

  private RecordHeaders convert(List<Map<String, Object>> headerList) {
    if (headerList == null || headerList.isEmpty()) {
      return new RecordHeaders();
    }
    List<Header> headers = new ArrayList<>();
    for (Map<String, Object> map : headerList) {
      for (Map.Entry<String, Object> e : map.entrySet()) {
        try {
          headers.add(new RecordHeader(e.getKey(), objectMapper.writeValueAsBytes(e.getValue())));
        } catch (IOException ex) {
          LOGGER.debug(String.format("Error converting value of key '%s' to byte[]", e.getKey()));
        }
      }
    }
    return new RecordHeaders(headers);
  }
}
