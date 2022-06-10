package dev.vox.platform.kahpp.unit.configuration.throttle;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.throttle.Throttle;
import dev.vox.platform.kahpp.unit.ConstraintViolationTestAbstract;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

class ThrottleTest extends ConstraintViolationTestAbstract {
  @Test
  void canBeConstructed() {
    Throttle step = new Throttle("test", Map.ofEntries(Map.entry("recordsPerSecond", 10)));

    Set<ConstraintViolation<Throttle>> violations = validator.validate(step);
    assertThat(violations).hasSize(0);
  }

  @Test
  void containsRateLimiterConfig() {
    Throttle step = new Throttle("test", Map.ofEntries(Map.entry("recordsPerSecond", 10)));

    assertThat(step.getRecordsPerSecond()).isEqualTo(10);
  }
}
