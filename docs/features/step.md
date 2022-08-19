# What is a Step?

We can think of a _step_ as a single logic action in the pipeline.  
In other words, a _step_ represents the smallest unit that, combined with other _steps_, shapes the pipeline.  

## Step configuration

All steps contains these fields: `name`, `type` and `config`.

```yaml
steps:
  - name: stepOneName
    type: step.one.type
    config:
      ### Configuration here
  - name: stepTwoName
    type: step.two.type
    config:
      ### Configuration here
```
