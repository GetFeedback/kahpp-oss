package dev.vox.platform.kahpp.configuration.http.client;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public final class Response {
  private final Map<String, String> headers;
  private final int statusCode;
  private final String body;

  Response(final Map<String, String> headers, final int statusCode, final String body) {
    this.headers = headers;
    this.statusCode = statusCode;
    this.body = body;
  }

  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(headers);
  }

  public Optional<String> getBody() {
    return Optional.ofNullable(body);
  }

  public int getStatusCode() {
    return statusCode;
  }
}
