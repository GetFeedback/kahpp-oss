package dev.vox.platform.kahpp.configuration.conditional;

import dev.vox.platform.kahpp.streams.KaHPPRecord;

public interface Condition {
  Condition ALWAYS = record -> true;

  boolean test(KaHPPRecord record);
}
