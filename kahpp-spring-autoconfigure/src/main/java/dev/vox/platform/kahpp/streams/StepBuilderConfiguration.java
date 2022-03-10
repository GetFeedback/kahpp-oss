package dev.vox.platform.kahpp.streams;

import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.processor.StepProcessorSupplier;
import dev.vox.platform.kahpp.processor.jmespath.ArrayDiffFunction;
import dev.vox.platform.kahpp.processor.jmespath.ArrayIntersectFunction;
import dev.vox.platform.kahpp.processor.jmespath.CopyKeyToPropertyFunction;
import dev.vox.platform.kahpp.processor.jmespath.NowFunction;
import io.burt.jmespath.RuntimeConfiguration;
import io.burt.jmespath.function.FunctionRegistry;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"dev.vox.platform.kahpp.configuration"})
public class StepBuilderConfiguration {

  @Bean
  public JacksonRuntime jacksonRuntime() {
    FunctionRegistry defaultFunctions = FunctionRegistry.defaultRegistry();
    FunctionRegistry extend =
        defaultFunctions.extend(
            new ArrayIntersectFunction(),
            new ArrayDiffFunction(),
            new CopyKeyToPropertyFunction(),
            new NowFunction());

    RuntimeConfiguration configuration =
        new RuntimeConfiguration.Builder().withFunctionRegistry(extend).build();

    return new JacksonRuntime(configuration);
  }

  @Bean
  public StepBuilderMap stepBuilderMap(
      @Autowired List<StepProcessorSupplier<? extends Step>> stepToKStreamList) {
    StepBuilderMap stepBuilderMap = new StepBuilderMap();
    stepToKStreamList.forEach(
        stepToKStream -> stepBuilderMap.put(stepToKStream.getType(), stepToKStream));

    return stepBuilderMap;
  }

  public static class StepBuilderMap
      extends HashMap<Class<? extends Step>, StepProcessorSupplier<? extends Step>>
      implements Map<Class<? extends Step>, StepProcessorSupplier<? extends Step>> {
    private static final long serialVersionUID = 3334301402583863261L;

    public StepProcessorSupplier<? extends Step> get(Step step) {
      return this.get(step.getClass());
    }

    public StepProcessorSupplier<? extends Step> get(Class<? extends Step> stepClass) {
      if (super.containsKey(stepClass)) {
        return super.get(stepClass);
      } else if (super.containsKey(stepClass.getSuperclass())) {
        return super.get(stepClass.getSuperclass());
      }
      return super.get(stepClass.getInterfaces()[0]);
    }
  }
}
