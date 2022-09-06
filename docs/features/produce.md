# Produce

## Declare a topic

First, we need to declare the `topics` on the Kahpp instance.

It is mandatory to have one source topic named `source`

For example:
```yaml
kahpp:
  topics:
    source: sport.topic
    sport-outdoors: sport-outdoors.topic
    sport-indoors: sport-indoors.topic
    error: error.topic
```

## Produce to a topic

Permits to sink records to a specific topic.

### Usage example

```yaml
    - name: produceRecordToSinkTopic
      type: dev.vox.platform.kahpp.configuration.topic.ProduceToTopic
      config:
        topic: sport-outdoors
```

## Routing

Permits routing records to specific topic using [`jmespath`](https://jmespath.org/).  
The routes are declared under `routes` with `jmesPath` as a matching condition and `topic` as the name for the topic to route the message to.

If no route is matched, the record lands in `errorTopic`


### Usage examples

A filter-like JMESPath evaluation using declared topics for routing

```yaml
- name: produceActionToSinkTopic
  type: dev.vox.platform.kahpp.configuration.topic.ProduceToTopicByRoute
  config:
    errorTopic: error
    routes:
      - jmesPath: value.sportType == 'outdoors'
        topic: sport-outdoors
      - jmesPath: value.sportType == 'indoors'
        topic: sport-indoors
```
