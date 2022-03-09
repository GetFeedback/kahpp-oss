package dev.vox.platform.kahpp.processor;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.streams.Instance.Config;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.To;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class Start extends AbstractProcessor<JsonNode, JsonNode> {

  public static final String MDC_KAFKA_MESSAGE_KEY = "kafka_message_key";

  private static final Logger LOGGER = LoggerFactory.getLogger(Start.class);

  private final transient Config config;

  public Start(Config config) {
    this.config = config;
  }

  @Override
  public void process(JsonNode key, JsonNode value) {
    MDC.clear();

    MDC.put("kafka_streams_application_id", config.getApplicationID());
    MDC.put("kahpp_group", config.getGroup());
    MDC.put("kahpp_name", config.getName());

    // can be overwritten by transform steps
    MDC.put(MDC_KAFKA_MESSAGE_KEY, key.toString());

    LOGGER.debug("Start processing Record");

    context().forward(key, value, To.all());
  }
}
