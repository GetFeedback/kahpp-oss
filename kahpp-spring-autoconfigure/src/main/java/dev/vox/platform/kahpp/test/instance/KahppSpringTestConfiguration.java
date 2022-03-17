package dev.vox.platform.kahpp.test.instance;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("test")
@TestConfiguration
public class KahppSpringTestConfiguration {
  @Bean
  public ExecutorService executorService() {
    return Executors.newSingleThreadExecutor();
  }
}
