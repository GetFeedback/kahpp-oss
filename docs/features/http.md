# HTTP Step

Kahpp makes it possible to reach one or more HTTP Endpoints.  

### Usage

First, we need to declare the API on the Kahpp instance to do that.

For example:
```yaml
kahpp:
  apis:
    my-dummy-api:
      basePath: http://my-dummy-api
      options: # Optional
        rateLimit:
          requestsPerSecond: 20
          warmUpMillis: 2000
        connection:
          connectTimeoutMillis: 300
          socketTimeoutMs: 1500
        retries:
          retryOnTimeout: true
          statusCodeRetryTimeSeedInMs: 500
          statusCodeRetryTimeCapInMs: 5000
          statusCodes:
            - statusCodeStart: 500
              statusCodeInclusiveEnd: 599
              retries: 10
```
Then in the steps, we may decide how to use it and how to handle the response.

## Ok or Produce Error
In case of error, it routes the message to a specific topic.

Simple Http Call
```yaml
  - name: httpCall
    type: dev.vox.platform.kahpp.configuration.http.OkOrProduceError
    config:
      api: my-dummy-api
      path: /my/path
      topic: error
```

Http Call with response handler

Update the field `name` with the HTTP Response
```yaml
  - name: httpCall
    type: dev.vox.platform.kahpp.configuration.http.OkOrProduceError
    config:
      api: my-dummy-api
      path: /my/path
      topic: error
      responseHandler:
        type: RECORD_UPDATE
        jmesPath: value.name
```

## Handle by Status Code
The response can also be handled in different ways based on the status code.

```yaml
  - name: httpCall
    type: dev.vox.platform.kahpp.configuration.http.HandleByStatusCode
    config:
      api: my-dummy-api
      path: /my/path
      topic: error
      responseHandlers:
        - statusCodeStart: 412
          statusCodeInclusiveEnd: 412
          responseHandler:
            type: RECORD_ROUTE
            topics:
              - retriableErrors
              - error
        - statusCodeStart: 422
          statusCodeInclusiveEnd: 422
          responseHandler:
            type: RECORD_ROUTE
            topics:
              - unretriableErrors
              - error
```

## Optional parameters

| name                 | default | description                                                                                |
|----------------------|---------|--------------------------------------------------------------------------------------------|
| forwardRecordOnError | false   | In case of error, don't stop the pipeline, so the record will be forward to the next step. |
| condition            |         | JMESPath expression, if false the step will be skipped                                     |

## Response Handlers

| Name                 | Description                                      | Additional Parameter | Parameter description                   |
|----------------------|--------------------------------------------------|----------------------|-----------------------------------------|
| RECORD_FORWARD_AS_IS | Forward record to the next step                  |                      |                                         |
| RECORD_UPDATE        | Replace record field with the HTTP Response body | jmesPath             | Field to replace with the HTTP Response |
| RECORD_TERMINATE     | Don't forward record to the next step            |                      |                                         |
| RECORD_ROUTE         | Route record to specific topics                  | topics               | List of topics to route the record to   |
