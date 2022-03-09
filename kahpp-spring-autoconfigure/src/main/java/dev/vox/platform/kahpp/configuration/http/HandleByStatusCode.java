package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.configuration.util.Range;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HandleByStatusCode extends OkOrProduceError implements HttpCall {

  public static final String RESPONSE_HANDLERS = "responseHandlers";

  @SuppressWarnings("unchecked")
  public HandleByStatusCode(String name, Map<String, ?> config) {
    super(name, config);

    if (config.containsKey(RESPONSE_HANDLERS)) {
      this.responseHandler =
          new ResponseHandlerByStatusCode(
              (Map<Range, ResponseHandler>) config.get(RESPONSE_HANDLERS));
    }
  }

  @Override
  public Set<TopicEntry.TopicIdentifier> eligibleSinkTopics() {
    Set<TopicEntry.TopicIdentifier> topics = new HashSet<>(super.eligibleSinkTopics());
    topics.addAll(((ResponseHandlerByStatusCode) this.responseHandler).getTopics());
    return Collections.unmodifiableSet(topics);
  }
}
