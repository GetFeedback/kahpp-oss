package dev.vox.platform.kahpp.configuration.topic;

import dev.vox.platform.kahpp.configuration.topic.validation.SourceTopicIsPresent;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.Size;

@SourceTopicIsPresent
@Size(min = 1, max = 100)
public final class TopicsMap extends HashMap<String, TopicEntry>
    implements Map<String, TopicEntry>, Serializable {
  private static final long serialVersionUID = -4917327073874163327L;

  public static final String TOPIC_SOURCE_IDENTIFIER = "source";

  /** Necessary due to Spring ConfigurationProperties */
  public TopicsMap() {}

  public TopicsMap(Map<String, String> topics) {
    topics.forEach(
        (identifier, name) -> {
          TopicEntry topicEntry = new TopicEntry(identifier, name);
          super.put(topicEntry.getIdentifier().asString(), topicEntry);
        });
  }

  public TopicEntry getSource() {
    return this.get(TOPIC_SOURCE_IDENTIFIER);
  }

  public Map<String, TopicEntry> getSinkTopics() {
    return this.entrySet().stream()
        .filter(entry -> !entry.getKey().equals(TOPIC_SOURCE_IDENTIFIER))
        .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue));
  }

  public Set<String> all() {
    return this.values().stream().map(TopicEntry::getName).collect(Collectors.toUnmodifiableSet());
  }

  public TopicEntry get(String identifier) {
    if (!this.containsKey(identifier)) {
      throw new IllegalArgumentException(
          String.format("Could not find Topic with identifier: \"%s\"", identifier));
    }
    return super.get(identifier);
  }
}
