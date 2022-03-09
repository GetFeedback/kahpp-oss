package dev.vox.platform.kahpp.configuration.http;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;

/** Builds ResponseHandler from map of configs. */
public final class ResponseHandlerBuilder {

  public static final String TYPE = "type";
  public static final String JMES_PATH = "jmesPath";
  public static final String TOPICS = "topics";

  /** Builds ResponseHandler from map. */
  @Valid
  public static ResponseHandler build(Map<String, ?> config) {
    return new ResponseHandlerConfig(
            (String) config.get(TYPE),
            (String) config.get(JMES_PATH),
            config.containsKey(TOPICS)
                ? (Set<String>)
                    ((Map) config.get(TOPICS)).values().stream().collect(Collectors.toSet())
                : Collections.emptySet())
        .build();
  }
}
