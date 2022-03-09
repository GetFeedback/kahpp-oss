package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ProduceToTopic implements ProduceToStaticRoute {
  @NotBlank private transient String name;

  @NotNull private transient TopicIdentifier topic;

  public ProduceToTopic(String name, Map<String, ?> config) {
    this.configure(name, config);
  }

  private void configure(String name, Map<String, ?> config) {
    this.name = name;
    this.topic = (TopicIdentifier) config.get(ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC);
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public TopicIdentifier produceToSink(KaHPPRecord record) {
    return this.topic;
  }

  @Override
  public Set<TopicIdentifier> eligibleSinkTopics() {
    return Set.of(this.topic);
  }
}
