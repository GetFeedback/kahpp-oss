package dev.vox.platform.kahpp.configuration.transform;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.List;

public interface FlatRecordTransform extends Step {
  public List<KaHPPRecord> transform(JacksonRuntime runtime, KaHPPRecord record);
}
