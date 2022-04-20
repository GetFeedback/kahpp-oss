# Kahpp [![Coverage Status](https://coveralls.io/repos/github/GetFeedback/kahpp-oss/badge.svg?t=rUt4Ui)](https://coveralls.io/github/GetFeedback/kahpp-oss) [![check](https://github.com/GetFeedback/kahpp-oss/actions/workflows/check.yml/badge.svg)](https://github.com/GetFeedback/kahpp-oss/actions/workflows/check.yml)

### A Low-Code Real-Time Kafka Stream Processor

Kahpp is a self-service Kafka Stream processor capable of filtering, transforming and routing Kafka records in real-time with just a YAML file.  
Its most straightforward configuration lets you consume a topic and trigger HTTP API requests for each received message. At its more complex setup, it can filter, duplicate, route, re-process, and do much more based on a condition.

## Motivation

Everything Kahpp does is achievable with Kafka Streams, but that comes with an overhead of code and knowledge.  
The motivation of this project is to simplify this process by using only a configuration file.  
Offering a centrally deployed configuration based service allows teams to quickly bridge the gap from starting a project to consuming a topic and running their code. In addition, from an infrastructure perspective, it allows more stability and gains the benefits of using Java for Kafka interactions.

## Getting started :rocket:

Kahpp provides a ready to use Spring setup with a Gradle Plugin useful to manage the instances.
Here you can find a [Gradle example project](examples/kahpp-gradle-example).

Default location for an Instance is `kahpp/application.yaml`.  
It's possible to override the location using the `KAHPP_CONFIG_LOCATION` environment variable.  

#### Required configuration parameters

| YAML KEY                             | DESCRIPTION                                                                                                                                                                         |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| kahpp.group                          | Logical name of the group that owns the instance, it can be company or teams or domain etc.                                                                                         |
| kahpp.name                           | Name of the instance                                                                                                                                                                |
| kahpp.topics.source                  | Source topic, the entry point of the pipeline                                                                                                                                       |
| kahpp.streamsConfig.bootstrapServers | The Kafka bootstrap servers.                                                                                                                                                        |
| kahpp.streamsConfig.*                | All other kahpp.streamsConfig, follow the official Kafka configuration parameter [reference](https://kafka.apache.org/10/documentation/streams/developer-guide/config-streams.html) |
| kahpp.steps                          | List of the steps, describe the pipeline.                                                                                                                                           |

### What is a Kahpp Instance?

A _Kahpp Instance_ or just _Instance_ is a YAML file, essentially a configuration, but we can think of it as the description of the pipeline.

## Modules
There are three main modules in Kahpp.  
Here is a quick overview:

### [kahpp-spring-autoconfigure](kahpp-spring-autoconfigure)
This provides the Spring auto-configuration for Kahpp.  
Kahpp has a default setup like any well-known Spring Boot project, based on the auto-configuration concept to automatically create and wire all the needs to set up a new application.

### [kahpp-spring-starter](kahpp-spring-starter)
Spring Boot Starters are a set of convenient dependency descriptors that you can include in your application, there are plenty of Spring Starters out there, and we have one for Kahpp!  
You'll get everything for Kahpp and the related technology you need without copy-paste.  
So, if you want to get started using Kahpp, include the `kahpp-spring-starter` dependency in your project, and you are good to go.    
```
implementation "dev.vox.platform.kahpp:kahpp-spring-starter:${KAHPP_VERSION}"
```

### [kahpp-gradle-plugin](kahpp-gradle-plugin)
The Gradle Plugin to maximize the usage of Kahpp.  
We love easy things, and we love to make things easier for everyone.

#### Install 
As this plugin is in a GitHub artifactory we first have to configure the consumer Gradle to have our repository enabled:  
```
buildscript {
    repositories {
        maven {
          url "https://maven.pkg.github.com/GetFeedback/kahpp-oss"
        }
    }
    dependencies {
      classpath "dev.vox.platform.kahpp:kahpp-gradle-plugin:${KAHPP_VERSION}"
    }
}
```
After this block we're now able to apply the plugin:
```
apply plugin: 'dev.vox.platform.kahpp'
```

#### Tasks

The plugin provides a series of task that help development. 

| Name                  | Description                                      |
|-----------------------|--------------------------------------------------|
| copyConfigFromHelm    | Generate Test instances                          |
| detectHelmToTestDrift | Detect if Test and Actual instance don't match   |
| generateTestTasks     | Generate test task for each Instance on the repo |
| testAll               | Run each instance test                           |

List all tasks:
```
./gradlew tasks --group=kahpp
```

## Features

Here is an example of what Kahpp is capable of doing:

### Transformers
- `MoveField`: permits to move a field.
- `CopyField`: permits to copy a field.
- `DropField`: permits to remove a field.
- `ConvertZonedDateTimeField`: converts a date from one format to another.
- `ExtractField`: pulls a field out of a complex value and replaces the entire value with the extracted field.
- `InsertCurrentTimestampField`: add a new field on value with the current timestamp.
- `SplitValue`: split an array into values.
- `TimestampToValue`: copy timestamp of record into value.
- `UnwrapValue`: unwraps the content of one field to root value.
- `WrapValue`: wraps the current content in a single field.

### Filters
- `FilterField`: permits to filter records by specific field (key,value,timestamp) using `jmespath`.

### Throttle
- `Throttle`: Limit the output of a KaHPP instance by applying a rate limit using `recordsPerSecond`.

### JMESPath custom functions
We can make our filters more powerful using `JMESPath` functions.
- `now`: permits addition or subtraction from now time for different units.
  - For example, we want to know the value (epoch millis) of yesterday, we can use "`now('-P1D')` "the result will be the epoch in millisecond unit of yesterday.
  - The operation is in ISO 8601 duration format (see more [here](https://en.wikipedia.org/wiki/ISO_8601#Durations)).

### HTTP Calls
Kahpp makes it possible to reach one or more HTTP Endpoints.
We need to declare the API on the Kahpp instance to do that.

For example:
```yaml
  apis:
    my-dummy-api:
      basePath: http://my-dummy-api
      options:
        rateLimit:
          requestsPerSecond: 20
          warmUpMillis: 2000
        connection:
          connectTimeoutMillis: 300
          socketTimeoutMs: 1500
```
Then in the steps, we may decide how to use it and how to handle the response.

- `OkOrProduceError`: in case of error, it routes the message to a specific topic.
  ```yaml
  - name: httpCall
    type: dev.vox.platform.kahpp.configuration.http.OkOrProduceError
    config:
      api: my-dummy-api
      path: /my/path
      topic: error
  ```
  
## Release process
Read about our release process [here](RELEASE.md).

## Contributing to Kahpp :heart:

If you are here, you're thinking of giving your contribution to Kahpp, so thanks!  

Please, follow this list to have a smooth journey:  
* Read our [contributing guidelines](CONTRIBUTING.md) and our [code of conduct](CODE_OF_CONDUCT.md).
* Be sure to setup `gradle.properties` with required information documented in `gradle.properties.dist`.
* Clone the repo and make your contribution into a separate branch.
* Open for a PR. 
