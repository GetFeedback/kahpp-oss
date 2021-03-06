package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.RecordActionRoute;
import dev.vox.platform.kahpp.configuration.http.client.Response;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import java.util.Collections;
import java.util.Set;
import javax.validation.constraints.NotNull;

public class ResponseHandlerRecordRoute implements ResponseHandler {

  @NotNull private final transient Set<TopicEntry.TopicIdentifier> topics;

  public ResponseHandlerRecordRoute(Set<TopicEntry.TopicIdentifier> topics) {
    this.topics = Set.copyOf(topics);
  }

  @Override
  public RecordActionRoute handle(Response response) throws ResponseHandlerException {
    return new RecordActionRoute() {
      @Override
      public Set<TopicEntry.TopicIdentifier> routes() {
        return topics;
      }

      @Override
      public boolean shouldForward() {
        return false;
      }
    };
  }

  public Set<TopicEntry.TopicIdentifier> getTopics() {
    return Collections.unmodifiableSet(topics);
  }
}
