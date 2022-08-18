# Throttle
Limit the output of a KaHPP instance by applying a rate limit using `recordsPerSecond`.

```yaml
  - name: throttle
    type: dev.vox.platform.kahpp.configuration.throttle.Throttle
    config:
      recordsPerSecond: 10
```
