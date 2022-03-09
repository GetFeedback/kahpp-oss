package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.streams.processor.To;

public class TopicEntry extends SimpleEntry<TopicIdentifier, String>
    implements Map.Entry<TopicIdentifier, String> {

  private static final long serialVersionUID = -8313855124736315604L;

  public TopicEntry(String identifier, String name) {
    super(new TopicIdentifier(identifier), name);
  }

  public TopicIdentifier getIdentifier() {
    return super.getKey();
  }

  public String getName() {
    return super.getValue();
  }

  @Override
  public String setValue(String s) {
    throw new UnsupportedOperationException();
  }

  public static class TopicIdentifier {

    private final transient String identifier;

    public TopicIdentifier(String identifier) {
      this.identifier = identifier;
    }

    public String asString() {
      return identifier;
    }

    /** A Processor should point to the Sink Node identifier */
    public To to() {
      return To.child(this.asString());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TopicIdentifier)) {
        return false;
      }
      TopicIdentifier that = (TopicIdentifier) o;

      return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
      return Objects.hash(identifier);
    }
  }
}
