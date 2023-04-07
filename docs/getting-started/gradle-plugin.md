# Gradle Plugin

While using the Kahpp plugin can be helpful for managing cases, it is not required to run a Kahpp instance.  
The plugin provides additional tools that can be useful in certain scenarios, but it is not necessary for basic use of the platform.

If you prefer to use Kahpp without the plugin, we've provided a sample Gradle project on our GitHub page that shows you how to set up and run a Kahpp instance without the plugin.  
The project includes a sample YAML file and detailed instructions on how to configure and deploy your Kahpp instance using Gradle.

To access the full example, please visit the following link on our GitHub repository: https://github.com/kahpp/kahpp/tree/main/examples/kahpp-gradle-example. 

``` yaml
--8<-- "./examples/kahpp-gradle-example/build.gradle"
```

## Tasks

List all tasks:
```
./gradlew tasks --group=kahpp
```

The plugin provides a series of task that help development.

| Name                  | Description                                      |
|-----------------------|--------------------------------------------------|
| copyConfigFromHelm    | Generate Test instances                          |
| detectHelmToTestDrift | Detect if Test and Actual instance don't match   |
| generateTestTasks     | Generate test task for each Instance on the repo |
| testAll               | Run each instance test                           |
