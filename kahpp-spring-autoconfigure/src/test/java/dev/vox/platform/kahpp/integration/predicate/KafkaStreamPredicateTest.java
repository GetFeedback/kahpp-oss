package dev.vox.platform.kahpp.integration.predicate;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.predicate.PredicateOrProduceError;
import dev.vox.platform.kahpp.configuration.topic.ProduceToStaticRoute;
import dev.vox.platform.kahpp.configuration.topic.ProduceToTopic;
import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import dev.vox.platform.kahpp.integration.KafkaStreamsTest;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaStreamPredicateTest extends KafkaStreamsTest {

  @Override
  protected Map<String, String> getTopics() {
    return Map.of(
        "source", AbstractKaHPPTest.TOPIC_SOURCE,
        "error", AbstractKaHPPTest.TOPIC_ERROR,
        "sink", AbstractKaHPPTest.TOPIC_SINK);
  }

  @Override
  protected List<StepConfiguration<? extends Step>> getSteps() {
    StepConfiguration<PredicateOrProduceError> notWebPassiveToError =
        new StepConfiguration<>(
            PredicateOrProduceError.class,
            "notWebPassiveToError",
            Map.of(
                "jmesPath",
                "value.payload.channel.name == 'collection_6'",
                ProduceToStaticRoute.STEP_CONFIGURATION_TOPIC,
                "error"));

    final StepConfiguration<ProduceToTopic> produceToTopicStep =
        new StepConfiguration<>(
            ProduceToTopic.class, "produceRecordToSinkTopic", Map.of("topic", "sink"));

    return List.of(notWebPassiveToError, produceToTopicStep);
  }
}
