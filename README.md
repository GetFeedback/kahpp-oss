<p align="center"><img height="200" src="docs/assets/kahpp-logo.svg"/></p>


# Kahpp [![Coverage Status](https://coveralls.io/repos/github/kahpp/kahpp/badge.svg?t=rUt4Ui)](https://coveralls.io/github/kahpp/kahpp) [![check](https://github.com/kahpp/kahpp/actions/workflows/check.yml/badge.svg)](https://github.com/kahpp/kahpp/actions/workflows/check.yml)

# A Low-Code Real-Time Kafka Stream Processor

Kahpp is a self-service Kafka Stream processor capable of filtering, transforming and routing Kafka records in real-time with a YAML file.  
Its most straightforward configuration lets you consume a topic and trigger HTTP API requests for each received message. At its more complex setup, it can filter, duplicate, route, re-process, and do much more based on a condition.

Take a look at the [reference documentation](https://kahpp.github.io/kahpp).

## Modules
There are various modules in Kahpp.  
Here is a quick overview:

### [kahpp-spring-autoconfigure](kahpp-spring-autoconfigure)
This provides the Spring auto-configuration for Kahpp.  
Kahpp has a default setup like any well-known Spring Boot project, based on the auto-configuration concept to automatically create and wire all the needs to set up a new application.

### [kahpp-spring-starter](kahpp-spring-starter)
Spring Boot Starters are a set of convenient dependency descriptors that you can include in your application, there are plenty of Spring Starters out there, and we have one for Kahpp!  
You'll get everything for Kahpp and the related technology you need without copy-paste.  
So, if you want to get started using Kahpp, include the `kahpp-spring-starter` dependency in your project, and you are good to go.

### [kahpp-gradle-plugin](kahpp-gradle-plugin)
The Gradle Plugin to maximize the usage of Kahpp.  
We love easy things, and we love to make things easier for everyone.

### [kahpp-docker](kahpp-docker)
It's a Spring Boot app that leverages the task 'bootBuildImage' to build the docker image, it uses [Buildpacks](https://buildpacks.io/) under the hood and uses [paketobuildpacks](https://paketo.io/docs/howto/java/) as the default builder.  
To build the docker image use:
```
./gradlew bootBuildImage
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

## Powered By
<a href="https://www.momentive.ai/"><img height="100" src="docs/assets/momentive-logo.svg"/></a>
