# Run Kahpp with Docker :rocket:

Running Kahpp using Docker is the easiest and fastest way to use and play with it.  
All we need is just our Yaml file of a Kahpp instance and a Kafka cluster available.

Here you can find the full example [here](https://github.com/GetFeedback/kahpp-oss/tree/main/examples/kahpp-docker-example).

## Docker 

```shell
docker run -v instance.yaml:/kahpp/application.yaml ghcr.io/getfeedback/kahpp-oss
```

## Docker Compose

```yaml
--8<-- "./examples/kahpp-docker-example/docker-compose.yaml"
```
