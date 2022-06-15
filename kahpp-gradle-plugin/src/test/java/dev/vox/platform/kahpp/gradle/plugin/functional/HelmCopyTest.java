package dev.vox.platform.kahpp.gradle.plugin.functional;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelmCopyTest {
  private final transient File testProjectDir =
      new File(ClassLoader.getSystemResource("functional/helm-copy").getFile());
  private final transient File resourcesOutputDir =
      new File(testProjectDir, "src/test/resources/functional");

  @BeforeEach
  void setUp() throws IOException {
    cleanUpOutputFiles();
  }

  private void cleanUpOutputFiles() throws IOException {
    if (!resourcesOutputDir.exists()) return;
    Path src = resourcesOutputDir.toPath().getParent().getParent().getParent();
    Files.walk(src).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
  }

  @Test
  void canDetectDriftFromHelmToTestsState() {
    BuildResult result =
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("detectHelmToTestDrift")
            .buildAndFail();

    BuildTask taskResult = result.task(":detectHelmToTestDrift");
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getOutcome()).isEqualTo(TaskOutcome.FAILED);

    assertThat(result.getOutput())
        .contains(
            "Found KaHPP configuration: 'kahpp-plugin-test-copying-config-from-helm'",
            "Instance 'kahpp-plugin-test-copying-config-from-helm' was not tested");
  }

  @Test
  void canDetectAndCopyHelmKaHPPConfiguration() {
    BuildResult result =
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir)
            .withArguments("--rerun-tasks", "copyConfigFromHelm")
            .build();

    BuildTask taskResult = result.task(":copyConfigFromHelm");
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

    String instanceName = "kahpp-plugin-test-copying-config-from-helm";

    assertThat(result.getOutput())
        .contains("Found KaHPP configuration: '" + instanceName + "'", "Creating directory:");

    File outputDir = new File(resourcesOutputDir, instanceName);
    assertThat(outputDir).exists();
    assertThat(outputDir).isDirectory();

    assertThat(new File(outputDir, "kahpp-instance.yaml")).isFile();
  }
}
