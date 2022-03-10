package dev.vox.platform.kahpp.configuration.http;

import java.util.Map;

public final class SimpleHttpCall extends AbstractHttpCall implements HttpCall {

  public SimpleHttpCall(String name, Map<String, ?> config) {
    super(name, config);
  }
}
