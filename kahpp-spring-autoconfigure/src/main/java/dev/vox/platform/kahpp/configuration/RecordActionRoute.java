package dev.vox.platform.kahpp.configuration;

import dev.vox.platform.kahpp.configuration.topic.TopicEntry;
import java.util.Set;

public interface RecordActionRoute extends RecordAction {
  Set<TopicEntry.TopicIdentifier> routes();
}
