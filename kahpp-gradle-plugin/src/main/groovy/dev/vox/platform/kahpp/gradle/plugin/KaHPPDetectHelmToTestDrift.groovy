package dev.vox.platform.kahpp.gradle.plugin

import org.gradle.api.file.FileType
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges

import static org.assertj.core.api.Assertions.assertThat

abstract class KaHPPDetectHelmToTestDrift extends KaHPPHelmYamlAbstractTask {
    @TaskAction
    void execute(InputChanges inputChanges) {
        inputChanges.getFileChanges(helmDir).each { change ->
            if (change.fileType == FileType.DIRECTORY) return
                if (!change.file.name.matches(~/.*.y*ml/)) return
                if (change.file.parentFile.name != getEnv()) return
                if (change.changeType == ChangeType.REMOVED) return

                File helmValuesFile = new File(change.normalizedPath)

            KaHPPInstance expectedInstance = getInstance(helmValuesFile)
            String instanceName = expectedInstance.getCanonicalName()
            println "Found KaHPP configuration: '${instanceName}'"
            File instanceOutputDir = new File(outputDir.get().toString(), instanceName)

            assertThat(instanceOutputDir)
                    .as("Instance '%s' was not tested", instanceName)
                    .isDirectory()

            File actualInstanceFile = new File(instanceOutputDir, "kahpp-instance.yaml")
            assertThat(actualInstanceFile)
                    .as("Instance '%s' was not tested", instanceName)
                    .isFile()

            Object actualInstance = yaml.load(actualInstanceFile.newDataInputStream());
            assertThat(yaml.dump(actualInstance))
                    .as("Instance '%s' has drifted from Helm", instanceName)
                    .isEqualTo(yaml.dump(expectedInstance))
        }
    }
}
