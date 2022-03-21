package dev.vox.platform.kahpp.configuration.throttle;

import dev.vox.platform.kahpp.configuration.Step;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

public class Throttle implements Step {
  @NotBlank private final transient String name;
  @Positive private final transient int recordsPerSecond;

  public Throttle(String name, Map<String, ?> config) {
    this.name = name;
    this.recordsPerSecond = (int) config.get("recordsPerSecond");
  }

  @Override
  public String getName() {
    return name;
  }

  public int getRecordsPerSecond() {
    return recordsPerSecond;
  }
}
