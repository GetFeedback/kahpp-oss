package dev.vox.platform.kahpp.gradle.plugin

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.testing.Test
import org.springframework.boot.gradle.plugin.SpringBootPlugin

class KaHPPGradlePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(JavaLibraryPlugin)
        project.pluginManager.apply(SpringBootPlugin)
        project.pluginManager.apply(DependencyManagementPlugin)

        project.tasks.getByName("bootJar").configure {
            mainClass = "dev.vox.platform.kahpp.Application"
        }

        project.task("detectHelmToTestDrift", type: KaHPPDetectHelmToTestDrift) {
            setGroup("kahpp")

            helmDir.set(new File("${project.projectDir}/helm/values"))
            outputDir.set(new File("${project.projectDir}/src/test/resources/functional"))
        }

        project.task("copyConfigFromHelm", type: KaHPPCopyConfigFromHelm) {
            setGroup("kahpp")

            helmDir.set(new File("${project.projectDir}/helm/values"))
            outputDir.set(new File("${project.projectDir}/src/test/resources/functional"))
        }

        def generateTestTasks = project.task("generateTestTasks") {
            setGroup("kahpp")
            def instancesDirs = []
            def functionalTestsDir = new File("${project.projectDir}/src/test/resources/functional")

            if (functionalTestsDir.isDirectory()) {
                functionalTestsDir
                        .eachDir {
                            instancesDirs.push(it.canonicalFile.name)
                            inputs.dir(it)
                        }
            }

            instancesDirs.each { kahppInstance ->
                String taskName = "test-${kahppInstance}"
                project.task(taskName, type: KaHPPTest) {
                    setGroup("kahpp")
                    setDescription("Runs functional KaHPP tests for ${taskName}")
                    File buildDir = new File("${project.buildDir}/kahpp/${kahppInstance}")
                    doFirst {
                        println "Running tests for KaHPP instance: ${kahppInstance}"
                        systemProperty("kahpp-test.instance", kahppInstance)
                        systemProperty("kahpp-test.build-dir", buildDir.absolutePath)
                        new File("${buildDir}/pacts").mkdirs()
                    }
                    useJUnitPlatform()
                    testClassesDirs = project.sourceSets.test.output.classesDirs
                    classpath = project.sourceSets.test.runtimeClasspath
                    outputs.upToDateWhen { false }
                    // Ensures that even when changing the kahpp files, tests run
                    mustRunAfter project.getTasksByName("test", false).find()
                    //                    filter {
                    //                        includeTestsMatching "dev.vox.platform.kahpp.instance.*"
                    //                    }
                }
            }
        }

        def testAll = project.task("testAll", type: Test) {
            setGroup("kahpp")
            project.tasks.withType(KaHPPTest).findAll().each { testTask ->
                it.finalizedBy(testTask)
            }
        }

        testAll.dependsOn(generateTestTasks)
    }
}
