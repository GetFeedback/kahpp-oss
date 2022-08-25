# Routing

Permits routing records to specific topic using [`jmespath`](https://jmespath.org/).  
The routes are declared under `routes` with `jmesPath` as a matching condition and `topic` as the name for the topic to route the message to.

If no route is matched, the record lands in `errorTopic`
### Usage examples

A filter-like JMESPath evaluation 

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
