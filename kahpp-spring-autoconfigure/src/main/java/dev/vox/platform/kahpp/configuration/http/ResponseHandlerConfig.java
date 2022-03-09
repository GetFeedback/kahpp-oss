package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.http.validation.ValidResponseHandler;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.NotNull;

@ValidResponseHandler
public final class ResponseHandlerConfig {

  private final transient String type;
  private final transient String jmesPath;
  private final transient Set<TopicEntry.TopicIdentifier> topics;

  public ResponseHandlerConfig(String type, String jmesPath, Set<String> topics) {
    this.type = type;
    this.jmesPath = jmesPath;
    this.topics =
        topics != null
            ? topics.stream().map(TopicEntry.TopicIdentifier::new).collect(Collectors.toSet())
            : Collections.emptySet();
  }

  public String getType() {
    return type;
  }

  public String getJmesPath() {
    return jmesPath;
  }

  public Set<TopicEntry.TopicIdentifier> getTopics() {
    return Collections.unmodifiableSet(topics);
  }

  @NotNull
  public ResponseHandler build() {
    return ResponseHandlerType.valueOf(type).getHandler(jmesPath, topics);
  }

  public enum ResponseHandlerType {
    RECORD_FORWARD_AS_IS {
      @Override
      public ResponseHandler getHandler(String jmesPath, Set<TopicEntry.TopicIdentifier> topics) {
        return ResponseHandler.RECORD_FORWARD_AS_IS;
      }
    },
    RECORD_VALUE_REPLACE {
      @Override
      public ResponseHandler getHandler(String jmesPath, Set<TopicEntry.TopicIdentifier> topics) {
        return ResponseHandlerRecordUpdate.RECORD_VALUE_REPLACE;
      }
    },
    RECORD_UPDATE {
      @Override
      public ResponseHandler getHandler(String jmesPath, Set<TopicEntry.TopicIdentifier> topics) {
        return new ResponseHandlerRecordUpdate(jmesPath);
      }
    },
    RECORD_TERMINATE {
      @Override
      public ResponseHandler getHandler(String jmesPath, Set<TopicEntry.TopicIdentifier> topics) {
        return ResponseHandler.RECORD_TERMINATE;
      }
    },
    RECORD_ROUTE {
      @Override
      public ResponseHandler getHandler(String jmesPath, Set<TopicEntry.TopicIdentifier> topics) {
        return new ResponseHandlerRecordRoute(topics);
      }
    };

    public abstract ResponseHandler getHandler(
        String jmesPath, Set<TopicEntry.TopicIdentifier> topics);

    public static List<String> valuesAsString() {
      return Stream.of(ResponseHandlerType.values()).map(Enum::name).collect(Collectors.toList());
    }
  }
}
