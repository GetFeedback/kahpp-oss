# Gradle Plugin

Using the plugin to run a Kahpp instance is not required.  
Kahpp Plugin adds a set of tools useful to manage the cases.  
Here you can find a [Gradle example project](https://github.com/GetFeedback/kahpp-oss/tree/main/examples/kahpp-gradle-example).  

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
