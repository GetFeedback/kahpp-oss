package dev.vox.platform.kahpp.unit.configuration.conditional;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.conditional.Condition;
import dev.vox.platform.kahpp.configuration.conditional.ConditionFactory;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import org.junit.jupiter.api.Test;

class PathConditionTest {
  @Test
  void shouldTestRecordBasedOnCondition() {
    final ConditionFactory conditionFactory = new ConditionFactory(new JacksonRuntime());
    final Condition condition = conditionFactory.createCondition("value==`true`");

    assertThat(condition.toString()).isEqualTo("value==`true`");

    assertThat(
            condition.test(
                KaHPPRecord.build(NullNode.getInstance(), BooleanNode.getTrue(), 1584352842123L)))
        .isTrue();
    assertThat(
            condition.test(
                KaHPPRecord.build(NullNode.getInstance(), BooleanNode.getFalse(), 1584352842123L)))
        .isFalse();
  }
}
