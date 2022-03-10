package dev.vox.platform.kahpp.unit.step;

import static org.assertj.core.api.Assertions.*;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.predicate.PredicateBranch;
import dev.vox.platform.kahpp.step.StepMetricUtils;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StepMetricUtilsTest {

  private transient PredicateBranch predicateBranch;
  private transient HttpCall httpCall;

  @BeforeEach
  void setUp() {
    predicateBranch =
        new PredicateBranch() {
          @Override
          public String getName() {
            return "predicateBranchStep";
          }

          @Override
          public String getJmesPath() {
            return null;
          }

          @Override
          public boolean test(JacksonRuntime runtime, KaHPPRecord record) {
            return false;
          }

          @Override
          public boolean isRight() {
            return true;
          }
        };

    httpCall =
        new HttpCall() {
          @Override
          public String getName() {
            return "httpCallStep";
          }

          @Override
          public Either<Throwable, RecordAction> call(KaHPPRecord record) {
            return null;
          }

          @Override
          public boolean shouldForwardRecordOnError() {
            return false;
          }
        };
  }

  @Test
  void formatMetricName() {
    assertThat(StepMetricUtils.formatMetricName("awesome")).isEqualTo("kahppMetricPrefix.awesome");

    assertThat(StepMetricUtils.formatMetricName(httpCall)).isEqualTo("kahppMetricPrefix.http");
    assertThat(StepMetricUtils.formatMetricName(predicateBranch))
        .isEqualTo("kahppMetricPrefix.filter");

    assertThat(StepMetricUtils.formatMetricName(httpCall, "last"))
        .isEqualTo("kahppMetricPrefix.http.last");
    assertThat(StepMetricUtils.formatMetricName(predicateBranch, "last"))
        .isEqualTo("kahppMetricPrefix.filter.last");

    assertThatThrownBy(() -> StepMetricUtils.formatMetricName(() -> "notMapped"))
        .isInstanceOf(RuntimeException.class);
  }
}
