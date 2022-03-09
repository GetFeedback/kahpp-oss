package dev.vox.platform.kahpp.unit.configuration.predicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.predicate.PredicateOrProduceError;
import dev.vox.platform.kahpp.configuration.topic.TopicEntry.TopicIdentifier;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals"})
class PredicateOrProduceErrorTest extends ConstraintViolationTestAbstract {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private static final TopicIdentifier topicIdentifier = new TopicIdentifier("my-topic");
  private transient KaHPPRecord recordFooIsTrue;
  private transient KaHPPRecord recordFooIsFalse;

  @BeforeEach
  void setUp() {
    ObjectNode nodeTrue = MAPPER.createObjectNode();
    nodeTrue.put("foo", true);

    recordFooIsTrue = KaHPPRecord.build(null, nodeTrue, 1584352842123L);

    ObjectNode nodeFalse = MAPPER.createObjectNode();
    nodeFalse.put("foo", false);

    recordFooIsFalse = KaHPPRecord.build(null, nodeFalse, 1584352842123L);
  }

  @Test
  public void canConstruct() {
    PredicateOrProduceError predicateOrProduceError =
        new PredicateOrProduceError(
            "test-canConstruct",
            Map.of(
                "jmesPath", "path",
                "either", "right",
                "topic", topicIdentifier));
    Set<ConstraintViolation<PredicateOrProduceError>> violations =
        validator.validate(predicateOrProduceError);
    assertThat(violations).hasSize(0);
  }

  @Test
  public void canValidate() {
    PredicateOrProduceError predicateOrProduceError =
        new PredicateOrProduceError("test-canValidate", Map.of("either", "random"));
    Set<ConstraintViolation<PredicateOrProduceError>> violations =
        validator.validate(predicateOrProduceError);
    assertThat(violations).hasSize(3);
    Map<String, List<String>> actualViolations = validationsAsMap(violations);
    assertThat(actualViolations)
        .isEqualTo(
            Map.of(
                "jmesPath", List.of("must not be blank"),
                "either", List.of("must match \"left|right\""),
                "topic", List.of("must not be null")));
  }

  @Test
  public void leftPredicate() {
    PredicateOrProduceError predicate =
        new PredicateOrProduceError(
            "test-leftPredicate",
            Map.of(
                "jmesPath", "value.foo == `true`",
                "either", "left",
                "topic", topicIdentifier));

    assertThat(predicate.test(jacksonRuntime, recordFooIsTrue)).isTrue();
    assertThat(predicate.test(jacksonRuntime, recordFooIsFalse)).isFalse();
  }

  @Test
  public void rightPredicate() {
    PredicateOrProduceError predicate =
        new PredicateOrProduceError(
            "test-name",
            Map.of(
                "jmesPath", "value.foo == `true`",
                "either", "right",
                "topic", topicIdentifier));

    assertThat(predicate.test(jacksonRuntime, recordFooIsTrue)).isTrue();
    assertThat(predicate.test(jacksonRuntime, recordFooIsFalse)).isFalse();
  }
}
