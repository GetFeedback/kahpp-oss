package dev.vox.platform.kahpp.configuration.transform;

public class UnwrapDebeziumOutboxEnvelopeException extends RuntimeException {

  static final long serialVersionUID = -8490410025244657520L;

  public UnwrapDebeziumOutboxEnvelopeException(String message) {
    super(message);
  }
}
