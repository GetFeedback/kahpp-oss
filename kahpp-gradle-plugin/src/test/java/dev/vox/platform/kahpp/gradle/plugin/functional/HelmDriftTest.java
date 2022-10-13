package dev.vox.platform.kahpp.gradle.plugin.functional;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

class HelmDriftTest {
  @Test
  void canDetectDriftBetweenHelmAndTestInstances() {
    BuildResult result =
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(getTestProjectDir("drift-detected"))
            .withArguments("detectHelmToTestDrift")
            .buildAndFail();

    BuildTask taskResult = result.task(":detectHelmToTestDrift");
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getOutcome()).isEqualTo(TaskOutcome.FAILED);

    assertThat(result.getOutput())
        .contains(
            "Found KaHPP configuration: 'kahpp-plugin-test-drift-detected-from-helm-to-test'",
            "Instance 'kahpp-plugin-test-drift-detected-from-helm-to-test' has drifted from Helm",
            """
                expected:\s
                    "kahpp:
                      group: kahpp-plugin
                      name: test-drift-detected-from-helm-to-test
                      topics:
                        source: my-source-topic
                        sink: my-other-sink-topic
                      streamsConfig:
                        bootstrapServers:
                        - kafka:9092
                      steps:
                      - name: myNewStep
                        type: dev.vox.platform.kahpp.configuration.filter.FilterTombstone
                        config:
                          filterNot: true
                      - name: produceRecordToSinkTopic
                        type: dev.vox.platform.kahpp.configuration.topic.ProduceToTopic
                        config:
                          topic: sink
                    "
                   but was:\s
                    "kahpp:
                      group: kahpp-plugin
                      name: test-drift-detected-from-helm-to-test
                      topics:
                        source: my-source-topic
                        sink: my-sink-topic
                      streamsConfig:
                        bootstrapServers:
                        - my-staging-cluster:9092
                      steps:
                      - name: produceRecordToSinkTopic
                        type: dev.vox.platform.kahpp.configuration.topic.ProduceToTopic
                        config:
                          topic: sink
                    \"""");
  }

  @Test
  void passWhenThereIsNoDriftBetweenHelmAndTestInstances() {
    BuildResult result =
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(getTestProjectDir("no-drift"))
            .withArguments("detectHelmToTestDrift")
            .build();

    BuildTask taskResult = result.task(":detectHelmToTestDrift");
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getOutcome()).isIn(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE);
  }

  private File getTestProjectDir(String scenario) {
    return new File(ClassLoader.getSystemResource("functional/helm-drift/" + scenario).getFile());
  }
}
