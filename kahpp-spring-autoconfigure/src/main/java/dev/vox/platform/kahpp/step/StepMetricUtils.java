package dev.vox.platform.kahpp.step;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.http.HttpCall;
import dev.vox.platform.kahpp.configuration.predicate.PredicateBranch;
import io.micrometer.core.instrument.Tag;
import java.util.List;

public class StepMetricUtils {

  private static final String KAHPP_METRIC_PREFIX = "kahpp";
  private static final String METRIC_TAG_STEP_CLASS = "step";
  private static final String METRIC_TAG_STEP_NAME = "step_name";

  private StepMetricUtils() {}

  public static String formatMetricName(final String metricName) {
    return String.format("%s.%s", KAHPP_METRIC_PREFIX, metricName);
  }

  public static String formatMetricName(final Step step) {
    return String.format("%s.%s", KAHPP_METRIC_PREFIX, MetricType.forStep(step).type());
  }

  public static String formatMetricName(final Step step, final String metricName) {
    return String.format(
        "%s.%s.%s", KAHPP_METRIC_PREFIX, MetricType.forStep(step).type(), metricName);
  }

  public static List<Tag> getStepTags(final Step step) {
    return List.of(
        Tag.of(METRIC_TAG_STEP_CLASS, step.getClass().getSimpleName()),
        Tag.of(METRIC_TAG_STEP_NAME, step.getName()));
  }

  private enum MetricType {
    HTTP("http"),
    FILTER("filter");

    private final String metricType;

    public static MetricType forStep(Step step) {
      if (step instanceof HttpCall) {
        return MetricType.HTTP;
      }
      if (step instanceof PredicateBranch) {
        return MetricType.FILTER;
      }

      throw new RuntimeException("Step type not mapped to metric");
    }

    MetricType(String metricType) {
      this.metricType = metricType;
    }

    public String type() {
      return metricType;
    }
  }
}
