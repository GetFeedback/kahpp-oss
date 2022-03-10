package dev.vox.platform.kahpp.processor.jmespath;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class NowFunction extends BaseFunction {

  private Clock clock;

  public NowFunction() {
    super(ArgumentConstraints.typeOf(JmesPathType.STRING));
    clock = Clock.system(ZoneId.systemDefault());
  }

  public NowFunction(Clock clock) {
    super(ArgumentConstraints.typeOf(JmesPathType.STRING));
    this.clock = clock;
  }

  @Override
  protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
    String operation = runtime.toString(arguments.get(0).value());
    Duration parse = Duration.parse(operation);
    Instant plus = clock.instant().plus(parse);
    return runtime.createNumber(plus.toEpochMilli());
  }
}
