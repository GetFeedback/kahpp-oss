# Run Kahpp with Docker :rocket:

Running Kahpp using Docker is the quickest and simplest way to get started with the platform. To use Kahpp, all you need is a YAML configuration file for your Kahpp instance, as well as access to a Kafka cluster.

To make it even easier to get started with Kahpp, we've provided a full example on our GitHub page that shows you how to run Kahpp in Docker.  
The example includes everything you need to get started, including a sample YAML file and detailed instructions on how to set up your environment.

To access the full example, please visit the following link on our GitHub repository: https://github.com/kahpp/kahpp/tree/main/examples/kahpp-docker-example.

## Docker 

```shell
docker run -v instance.yaml:/kahpp/application.yaml ghcr.io/kahpp/kahpp
```

## Docker Compose

```yaml
--8<-- "./examples/kahpp-docker-example/docker-compose.yaml"
```
