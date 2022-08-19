<p align="center"><img height="100" src="assets/kahpp-logo.svg"/></p>

# A Low-Code Real-Time Kafka Stream Processor

Kahpp is a self-service Kafka Stream processor capable of filtering, transforming and routing Kafka records in real-time with a YAML file.  
Its most straightforward configuration lets you consume a topic and trigger HTTP API requests for each received message. At its more complex setup, it can filter, duplicate, route, re-process, and do much more based on a condition.

## Motivation

Everything Kahpp does is achievable with Kafka Streams, but that comes with an overhead of code and knowledge.  
The motivation of this project is to simplify this process by using only a configuration file.  
Offering a centrally deployed configuration based service allows teams to quickly bridge the gap from starting a project to consuming a topic and running their code. In addition, from an infrastructure perspective, it allows more stability and gains the benefits of using Java for Kafka interactions.

## Powered By
<a href="https://www.momentive.ai/"><img height="100" src="assets/momentive-logo.svg"/></a>
