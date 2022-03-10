package dev.vox.platform.kahpp.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Finalize extends AbstractProcessor<JsonNode, JsonNode> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Finalize.class);

  @Override
  public void process(JsonNode key, JsonNode value) {
    LOGGER.debug("Finished processing Record successfully");
  }
}
