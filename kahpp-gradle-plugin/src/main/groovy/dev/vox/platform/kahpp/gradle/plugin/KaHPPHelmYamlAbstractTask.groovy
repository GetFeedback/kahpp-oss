package dev.vox.platform.kahpp.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option
import org.gradle.work.Incremental
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

abstract class KaHPPHelmYamlAbstractTask extends DefaultTask {
    private transient String env = "staging"

    protected final transient Yaml yaml;

    KaHPPHelmYamlAbstractTask() {
        DumperOptions options = new DumperOptions()
        options.setIndent(2)
        options.setWidth(80)
        options.setSplitLines(false)
        options.setExplicitStart(false)
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN)
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        yaml = new Yaml(options)
    }

    @Incremental
    @InputDirectory
    abstract DirectoryProperty getHelmDir()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @Input
    String getEnv() {
        return this.env
    }

    @Option(option = "env", description = "The Helm environment to copy configuration files from")
    void setEnv(String env) {
        this.env = env
    }

    protected List<KaHPPInstance> getInstances(File helmValuesFile) {
        Map<String, Object> helmValues = yaml.load(helmValuesFile.newDataInputStream())
        Map<String, Map<String, Object>> kahpp = helmValues.get("kahpp")
        List<Map<String, Object>> instances = kahpp.get("instances")

        List<KaHPPInstance> kahppInstances = new ArrayList<>()

        instances.forEach {
            Map<String, Object> streamsConfig = it.get("streamsConfig")
            streamsConfig.put("bootstrapServers", List.of("kafka:9092"))
            kahppInstances.push(new KaHPPInstance(Map.of("kahpp", it)))
        }

        return kahppInstances.asUnmodifiable()
    }

    protected KaHPPInstance getInstance(File helmValuesFile) {
        Map<String, Object> helmValues = yaml.load(helmValuesFile.newDataInputStream())
        Map<String, Object> instance = helmValues.get("instance")
        Map<String, Object> streamsConfig = instance.get("streamsConfig")
        streamsConfig.put("bootstrapServers", List.of("kafka:9092"))
        return new KaHPPInstance(Map.of("kahpp", instance));
    }
}

class KaHPPInstance extends HashMap<String, Object> {
    KaHPPInstance(Map<String, Object> m) {
        super(m);
    }

    String getCanonicalName() {
        Map<String, Object> kahpp = this.get("kahpp")
        return kahpp.get("group").toString() + "-" + kahpp.get("name").toString()
    }
}
