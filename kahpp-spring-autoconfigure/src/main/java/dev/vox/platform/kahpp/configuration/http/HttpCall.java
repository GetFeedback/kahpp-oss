package dev.vox.platform.kahpp.configuration.http;

import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.vavr.control.Either;

public interface HttpCall extends Step {
  String RESPONSE_HANDLER_CONFIG = "responseHandler";

  Either<Throwable, RecordAction> call(KaHPPRecord record);

  boolean shouldForwardRecordOnError();
}
