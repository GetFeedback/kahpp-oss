# Contributing to Kahpp

Kahpp is released under the MIT license.  
If you want to contribute something, this document should help you get started before opening a pull request.

## Code of Conduct

This project adheres to the Contributor Covenant [code of conduct](CODE_OF_CONDUCT.md).  
By participating, you are expected to uphold this code.  Please report unacceptable behaviour.

## Using GitHub Issues

We use GitHub issues to track bugs and enhancements.

If you have a general usage question, please ask on [GitHub Discussions](https://github.com/kahpp/kahpp/discussions/categories/ask).  
The Kahpp team and the broader community monitor the section.

If you are reporting a bug, please help to speed up problem diagnosis by providing as much information as possible.
Ideally, that would include a small sample project that reproduces the problem.

## Reporting Security Vulnerabilities

If you think you have found a security vulnerability in Kahpp, please *DO NOT* disclose it publicly until we've had a chance to fix it.  
Please don't report security vulnerabilities using GitHub issues; instead, send descriptions of any vulnerabilities found to [oss-security@momentive.ai](mailto:oss-security@momentive.ai).  
Please include details on the software and hardware configuration of your system so that we can duplicate the issue being reported.

## Code Conventions and Housekeeping

None of these is essential for a pull request, but they will all help.  They can also be added after the original pull request but before a merge.

* To apply code formatting conventions, We use the [Spotless](https://github.com/diffplug/spotless) project.
You can format the code from the Gradle build by running `./gradlew spotlessApply`.
Note that if you have format violations, the build fails.
* The build includes test coverage rules.  Run `./gradlew jacocoTestCoverageVerification` if you want to check your changes are compliant.
* Make sure all new `.java` files have a Javadoc class comment with at least a paragraph on what the class is for.
* Add some Javadocs
* Update the [Readme file](README.md).
* A few unit tests would help a lot - someone has to do it.
* Verification tasks, including tests and Checkstyle, can be executed by running `./gradlew check` from the project root.
  Note that the Docker environment might affect the result of tests, make sure Docker is running in your machine.
* If no one else is using your branch, please rebase it against the project's current main branch (or other target branches).
* Please follow [atomic commits convention](https://www.pauline-vos.nl/atomic-commits/).

## NOTES

This document draws on the [Spring Boot CONTRIBUTING](https://github.com/spring-projects/spring-boot/blob/main/CONTRIBUTING.adoc) for content and inspiration. 
