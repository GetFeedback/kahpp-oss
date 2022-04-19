package dev.vox.platform.kahpp.configuration.http.client;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public final class Request {
  private final String method;
  private final String path;
  private final Map<String, String> headers;
  private final String body;

  Request(
      final String method,
      final String path,
      final Map<String, String> headers,
      final String body) {
    this.method = method;
    this.path = path;
    this.headers = headers;
    this.body = body;
  }

  Request(final String method, final String path, final Map<String, String> headers) {
    this(method, path, headers, null);
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(headers);
  }

  public Optional<String> getBody() {
    return Optional.ofNullable(body);
  }
}
