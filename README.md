# KaHPP
### A Low-Code Real-Time Kafka Stream Processor

[![Coverage Status](https://coveralls.io/repos/github/GetFeedback/kahpp-oss/badge.svg?t=rUt4Ui)](https://coveralls.io/github/GetFeedback/kahpp-oss)

KaHPP is a self-service Kafka Stream processor capable of filtering, transforming, route Kafka records in real-time with just a YAML file.  
At its simplest configuration, it allows you to consume a topic and trigger HTTP API requests for each received message. At its more complex configuration, it can filter, duplicate, route, re-process and do much more, also based on a condition.


## Motivation

Everything KaHPP does is achievable with Kafka Streams, but that comes with an overhead of code and knowledge, the motivation of this project is to simplify this process into a configuration file. Offering a centrally deployed configuration based service allows teams to quickly bridge the gap from the start of a project to consuming a topic and running their code. From an infrastructure perspective it allows more stability and gains the benefits of using Java for Kafka interactions.

_What do you mean by self-service_? Any team can roll out a new instance of KaHPP configured to their needs by opening a Pull Request to this repo.


## Features

Here is an example of what KaHPP is capable of doing:

### Transformers
- `MoveField`: permits to move a field.
- `CopyField`: permits to copy a field.
- `DropField`: permits to remove a field.
- `ConvertZonedDateTimeField`: converts a date from a format to another.
- `ExtractField`: pulls a field out of a complex value and replaces the entire value with the extracted field.
- `InsertCurrentTimestampField`: add a new field on value with the current timestamp.
- `SplitValue`: split an array into value.
- `TimestampToValue`: copy timestamp of record into value.
- `UnwrapValue`: unwraps the content of one field to root value.
- `WrapValue`: wraps the current content in a single field.

### Filters
- `FilterField`: permits to filter records by specific field (key,value,timestamp) using `jmespath`.

### JMESPath custom functions
We can make our filters more powerful using `JMESPath` functions.
- `now`: permits addition or subtraction from now time, for different units.
    - For example, we want to know the value (epoch millis) of yesterday we can just use ```now('-P1D')``` the result will be the epoch in millisecond of yesterday.
    - The operation is in ISO 8601 duration format (see more [here](https://en.wikipedia.org/wiki/ISO_8601#Durations)).

### HTTP Calls
With KaHPP is possible to reach one or more HTTP Endpoints.
To do that we need just to declare the API on the KaHPP instance.

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
Then in the steps we may decide how to use it and how to handle the response.

- `OkOrProduceError`: in case of error it routes the message to a specific topic.
  ```yaml
  - name: httpCall
    type: dev.vox.platform.kahpp.configuration.http.OkOrProduceError
    config:
      api: my-dummy-api
      path: /my/path
      topic: error
  ```

## Contributing to KaHPP

Be sure to setup `gradle.properties` with required information documented in `gradle.properties.dist`.

