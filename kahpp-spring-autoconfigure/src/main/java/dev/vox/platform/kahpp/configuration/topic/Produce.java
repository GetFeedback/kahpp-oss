package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import java.util.Set;

public interface Produce extends Step {
  Set<TopicEntry.TopicIdentifier> eligibleSinkTopics();

  TopicEntry.TopicIdentifier produceToSink(KaHPPRecord record);
}
