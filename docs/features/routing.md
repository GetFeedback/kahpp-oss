# Routing

Permits routing records to specific topic using [`jmespath`](https://jmespath.org/).  
The routes are declared under `routes` with `jmesPath` as a matching condition and `topic` as the name for the topic to route the message to.

If no route is matched, the record lands in `errorTopic`

### Usage

First, we need to declare the `topics` on the Kahpp instance.

It is mandatory to have one source topic named `source`

For example:
```yaml
kahpp:
  topics:
    source: response
    sport-outdoors: football
    sport-indoors: bowling
    error: action-discovery.error
```

### Usage examples

A filter-like JMESPath evaluation using declared topics for routing

```yaml
  - name: produceActionToSinkTopic
  type: dev.vox.platform.kahpp.configuration.topic.ProduceToTopicByRoute
  config:
    errorTopic: error
    routes:
      - jmesPath: value.actionType == 'email'
        topic: email
      - jmesPath: value.actionType == 'salesforce'
        topic: salesforce
      - jmesPath: value.actionType == 'slack'
        topic: slack
```
