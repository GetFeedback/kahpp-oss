package dev.vox.platform.kahpp.gradle.plugin.functional;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

class BasicTest {
  private final URL testProjectUrl = ClassLoader.getSystemResource("functional/basic");

  @Test
  public void hasAllTasks() {
    BuildResult result =
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(new File(testProjectUrl.getFile()))
            .withArguments("tasks", "--group=kahpp")
            .build();

    assertThat(result.getOutput())
        .contains("copyConfigFromHelm", "detectHelmToTestDrift", "generateTestTasks", "testAll");
  }
}
