package dev.vox.platform.kahpp.gradle.plugin

import org.gradle.api.file.FileType
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges

abstract class KaHPPCopyConfigFromHelm extends KaHPPHelmYamlAbstractTask {
    @TaskAction
    void execute(InputChanges inputChanges) {
        inputChanges.getFileChanges(helmDir).each { change ->
            if (change.fileType == FileType.DIRECTORY) return
                if (!change.file.name.matches(~/.*.y*ml/)) return
                if (change.file.parentFile.name != getEnv()) return
                if (change.changeType == ChangeType.REMOVED) return

                File helmValuesFile = new File(change.normalizedPath)
            KaHPPInstance instance = getInstance(helmValuesFile)

            def instanceName = instance.getCanonicalName()
            println "Found KaHPP configuration: '${instanceName}'"
            def instanceOutputDir = new File(outputDir.get().toString(), instanceName)
            if (!instanceOutputDir.exists()) {
                println "Creating directory: ${instanceOutputDir}"
                instanceOutputDir.mkdir()
            }
            FileWriter writer = new FileWriter(new File(instanceOutputDir, "kahpp-instance.yaml"))
            yaml.dump(instance, writer)
        }
    }
}
