package dev.vox.platform.kahpp.processor.jmespath;

import static java.lang.String.format;

import io.burt.jmespath.Adapter;
import io.burt.jmespath.JmesPathType;
import io.burt.jmespath.function.ArgumentConstraints;
import io.burt.jmespath.function.BaseFunction;
import io.burt.jmespath.function.FunctionArgument;
import java.util.*;

public class CopyKeyToPropertyFunction extends BaseFunction {
  public CopyKeyToPropertyFunction() {
    super(
        ArgumentConstraints.typeOf(JmesPathType.OBJECT),
        ArgumentConstraints.typeOf(JmesPathType.STRING));
  }

  @Override
  protected <T> T callFunction(Adapter<T> runtime, List<FunctionArgument<T>> arguments) {
    T map = arguments.get(0).value();
    T name = arguments.get(1).value();

    Map<T, T> accumulator = new LinkedHashMap<>();
    Map<T, T> entryCopy = new LinkedHashMap<>();
    for (T key : runtime.getPropertyNames(map)) {
      entryCopy.clear();
      T entry = runtime.getProperty(map, key);
      for (T property : runtime.getPropertyNames(entry)) {
        entryCopy.put(property, runtime.getProperty(entry, property));
      }

      if (entryCopy.containsKey(name)) {
        throw new IllegalArgumentException(format("Cannot override key: %s", name.toString()));
      }

      entryCopy.put(name, key);

      accumulator.put(key, runtime.createObject(entryCopy));
    }

    return runtime.createObject(accumulator);
  }
}
