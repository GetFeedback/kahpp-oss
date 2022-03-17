package dev.vox.platform.kahpp.test.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class KahppSpringTestConfigurationTest {

  @Test
  void executorService() {
    KahppSpringTestConfiguration kahppSpringTestConfiguration = new KahppSpringTestConfiguration();
    ExecutorService executorService = kahppSpringTestConfiguration.executorService();
    assertThat(executorService).isNotNull();
  }
}
