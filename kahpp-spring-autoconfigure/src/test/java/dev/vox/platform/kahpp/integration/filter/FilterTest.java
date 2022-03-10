package dev.vox.platform.kahpp.integration.filter;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.integration.AbstractKaHPPTest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FilterTest extends AbstractKaHPPTest {

  @Autowired private transient MeterRegistry meterRegistry;

  @Test
  void testFilterEnabledProducts() {
    processCollectionFixture("collection_5", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(1.0, true);

    processCollectionFixture("collection_6", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(1.0, true);
    assertKahppFilterEvaluationMetric(1.0, false);

    processCollectionFixture("collection_1", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(1.0, true);
    assertKahppFilterEvaluationMetric(2.0, false);

    processCollectionFixture("collection_3", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(1.0, true);
    assertKahppFilterEvaluationMetric(3.0, false);

    processCollectionFixture("collection_2", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(2.0, true);
    assertKahppFilterEvaluationMetric(3.0, false);

    processCollectionFixture("collection_4", TOPIC_SINK);
    assertKahppFilterEvaluationMetric(3.0, true);
    assertKahppFilterEvaluationMetric(3.0, false);
  }

  private void assertKahppFilterEvaluationMetric(double actual, boolean forwarded) {
    assertThat(actual)
        .isEqualTo(
            meterRegistry
                .get("kahppMetricPrefix.filter")
                .tag("step", "FilterValue")
                .tag("step_name", "getSupportedProducts")
                .tag("forwarded", Boolean.toString(forwarded))
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum());
  }
}
