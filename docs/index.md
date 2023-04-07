<p align="center"><img height="100" src="assets/kahpp-logo.svg"/></p>

# A Low-Code Real-Time Kafka Stream Processor

Kahpp is a user-friendly Kafka Stream processor that enables you to filter, transform, and route Kafka records in real-time using a simple YAML configuration file.   
With Kahpp, you can easily configure your setup to consume a topic and trigger HTTP API requests for each received message, or apply more complex operations such as filtering, duplication, routing, and re-processing based on specific conditions.

## Motivation

While Kafka Streams is a powerful tool for real-time stream processing, it can be challenging for developers who lack the required knowledge and expertise.  
Kahpp was created to simplify this process by eliminating the need for complex code and instead using a configuration file to specify the processing logic.  
This approach allows teams to quickly set up and deploy a Kafka Stream processor, without the need for deep knowledge of Kafka Streams and its associated code.    
Furthermore, by offering a centrally deployed configuration-based service, Kahpp provides greater stability and leverages the benefits of using Java for Kafka interactions.