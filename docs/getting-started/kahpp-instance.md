# What is a Kahpp Instance?

A _Kahpp Instance_ or just _Instance_ is a YAML file, essentially a configuration, but we can think of it as the description of the pipeline.

## Simple instance

Here a straightforward Kahpp instance that consumes from `source-topic` and sinks records on `sink-topic`.

``` yaml
--8<-- "./examples/kahpp-docker-example/kahpp/application.yaml"
```

## Kahpp instance location

Default location for an Instance is `kahpp/application.yaml`.  
It's possible to override the location using the `KAHPP_CONFIG_LOCATION` environment variable.  

## Configuration

| YAML KEY                             | DESCRIPTION                                                                                                                                                                         |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| kahpp.group                          | Logical name of the group that owns the instance, it can be company or teams or domain etc.                                                                                         |
| kahpp.name                           | Name of the instance                                                                                                                                                                |
| kahpp.topics.source                  | Source topic, the entry point of the pipeline                                                                                                                                       |
| kahpp.streamsConfig.bootstrapServers | The Kafka bootstrap servers.                                                                                                                                                        |
| kahpp.streamsConfig.*                | All other kahpp.streamsConfig, follow the official Kafka configuration parameter [reference](https://kafka.apache.org/10/documentation/streams/developer-guide/config-streams.html) |
| kahpp.steps                          | List of the steps, describe the pipeline.                                                                                                                                           |

