package dev.vox.platform.kahpp.processor.jmespath;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArrayDiffFunction extends BaseFunction {
  public ArrayDiffFunction() {
    super(
        ArgumentConstraints.typeOf(JmesPathType.ARRAY),
        ArgumentConstraints.typeOf(JmesPathType.ARRAY));
  }

  @Override
  protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
    T listA = arguments.get(0).value();
    T listB = arguments.get(1).value();

    Set<T> setDifference =
        runtime.toList(listA).stream()
            .distinct()
            .filter(element -> !runtime.toList(listB).contains(element))
            .collect(Collectors.toSet());

    return runtime.createArray(setDifference);
  }
}
