package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.topic.ProduceToStaticRoute;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

public class OkOrProduceError extends AbstractHttpCall implements ProduceToStaticRoute {

  @NotNull private final transient TopicEntry.TopicIdentifier topic;

  public OkOrProduceError(String name, Map<String, ?> config) {
    super(name, config);
    this.topic =
        (TopicEntry.TopicIdentifier) config.get(ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC);
  }

  @Override
  public Set<TopicEntry.TopicIdentifier> eligibleSinkTopics() {
    return Set.of(this.topic);
  }

  @Override
  public TopicEntry.TopicIdentifier produceToSink(KaHPPRecord record) {
    return this.topic;
  }
}
