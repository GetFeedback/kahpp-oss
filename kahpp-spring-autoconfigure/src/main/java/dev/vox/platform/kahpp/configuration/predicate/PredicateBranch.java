package dev.vox.platform.kahpp.configuration.predicate;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;

public interface PredicateBranch extends Step {

  String getJmesPath();

  boolean test(JacksonRuntime runtime, KaHPPRecord record);

  boolean isRight();
}
