# Filter

Permits to filter records by specific field (key, value, timestamp, etc.) using [`jmespath`](https://jmespath.org/).  

### Usage examples

A filter-like JMESPath evaluation 

```yaml
  - name: filterByValueName
    type: dev.vox.platform.kahpp.configuration.filter.FilterField
    config:
      jmesPath: "value.name == 'foo'"
```

More complex JMESPath expression  
Filter all records that contains `fooHeaderKey` in the headers.
```yaml
  - name: headerExists
    type: dev.vox.platform.kahpp.configuration.filter.FilterField
    config:
      jmesPath: >-
            { fooHeader: headers[]."fooHeaderKey" } | length(fooHeader) == `0`
```

## JMESPath functions
We can make our filters more powerful using `JMESPath` functions.
- `now`: permits addition or subtraction from now time for different units.
    - For example, we want to know the value (epoch millis) of yesterday, we can use "`now('-P1D')` "the result will be the epoch in millisecond unit of yesterday.
    - The operation is in ISO 8601 duration format (see more [here](https://en.wikipedia.org/wiki/ISO_8601#Durations)).

### Example 

Filter all records not older than 14 days ago
```yaml
  - name: filterByTimestamp
    type: dev.vox.platform.kahpp.configuration.filter.FilterField
    config:
      jmesPath: "timestamp > now('-P14D')"
```
