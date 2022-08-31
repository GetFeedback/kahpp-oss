# HTTP Step

Kahpp makes it possible to reach one or more HTTP Endpoints.  

## Declare API client

First, we need to declare the API client on the Kahpp instance.

For example:
```yaml
kahpp:
  apis:
    my-dummy-api: # Logical name of the client
      basePath: http://my-dummy-api
```
Then in the steps, we may decide how to use it and how to handle the response.

### Connection configurations

Set the connection configuration based on connect and socket timeouts.

```yaml
kahpp:
  apis:
    my-dummy-api: # Logical name of the client
      basePath: http://my-dummy-api
      options:
        connection:
          connectTimeoutMillis: 300
          socketTimeoutMs: 1500
```

| name                 | default | description                                                                                                                                                                 |
|----------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| connectTimeoutMillis | 500     | Determines the timeout in milliseconds until a connection is established.                                                                                                   |
| socketTimeoutMs      | 2000    | Defines the socket timeout in milliseconds, which is the timeout for waiting for data or, put differently, a maximum period inactivity between two consecutive data packets |


### Request headers

Set HTTP headers necessary for HTTP requests.

```yaml
kahpp:
  apis:
    my-dummy-api:
      basePath: http://my-dummy-api
      options:
        headers:
          Accept: text/html
          User-Agent: Mozilla/5.0
          Connection: keep-alive
```

Default header is `Accept:application/json`.

### Rate Limit

Configure rate limiter for HTTP requests.  
If unset, no http request rate limit will be in place.  

```yaml
kahpp:
  apis:
    my-dummy-api:
      basePath: http://my-dummy-api
      options:
        rateLimit:
          requestsPerSecond: 10
          warmUpMillis: 100
```

| name              | required | description                                                                                                     |
|-------------------|----------|-----------------------------------------------------------------------------------------------------------------|
| requestsPerSecond | yes      | set how many request are available per second.                                                                  |
| warmUpMillis      | no       | the duration in milliseconds of the period where the RateLimiter ramps up its rate, before reaching its stable. |


### Retry configuration

If unset, the default retry policy will be in place.

```yaml
kahpp:
  apis:
    my-dummy-api:
      basePath: http://my-dummy-api
      options:
        retries:
          retryOnTimeout: true
          statusCodeRetryTimeSeedInMs: 1000
          statusCodeRetryTimeCapInMs: 10000
          statusCodes:
            - statusCodeStart: 500
              statusCodeInclusiveEnd: 599
              retries: 10
```

| name                        | default                                                                                                                                             | description                                                             |
|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| connectionRetryCount        | 3                                                                                                                                                   | The amount of times to retry when the connection fails.                 |
| retryIdempotentRequests     | true                                                                                                                                                | True if it's OK to retry non-idempotent requests that have been sent.   |
| retryOnTimeout              | false                                                                                                                                               | True if it should retry timeout errors.                                 |
| statusCodeRetryTimeSeedInMs | 50                                                                                                                                                  | A seed in milliseconds used to calculate the backoff time.              |
| statusCodeRetryTimeCapInMs  | 1000                                                                                                                                                | The cap of the backoff time                                             |
| statusCodeRetryMemory       | 25                                                                                                                                                  | The amount of requests that contribute to the backoff time calculation. |
| statusCodes                 | - statusCodeStart: 500 <br/>&nbsp;&nbsp;statusCodeInclusiveEnd: 599 <br/>&nbsp;&nbsp;retries: 3 <br/>- statusCode: 429 <br/>&nbsp;&nbsp;retries: 10 | Strategy to retry responses with specific status codes.                 |


## Simple HTTP Call

This is the most straightforward HTTP call step.  

```yaml
  - name: simpleHttpCall
    type: dev.vox.platform.kahpp.configuration.http.SimpleHttpCall
    config:
      api: my-dummy-api
      path: /my/path
```

## Ok or Produce Error
In case of error, it routes the message to a specific topic.

Simple HTTP Call
```yaml
  - name: httpCall
    type: dev.vox.platform.kahpp.configuration.http.OkOrProduceError
    config:
      api: my-dummy-api
      path: /my/path
      topic: error
```

HTTP Call with response handler

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

| name                 | default | type               | description                                                                                |
|----------------------|---------|--------------------|--------------------------------------------------------------------------------------------|
| forwardRecordOnError | false   | boolean            | In case of error, don't stop the pipeline, so the record will be forward to the next step. |
| condition            |         | JMESPath           | JMESPath expression, if false the step will be skipped                                     |
| method               | POST    | POST,PUT,GET,PATCH | HTTP method to use for the request.                                                        |

## Response Handlers

| Name                 | Description                                      | Additional Parameter | Parameter description                   |
|----------------------|--------------------------------------------------|----------------------|-----------------------------------------|
| RECORD_FORWARD_AS_IS | Forward record to the next step                  |                      |                                         |
| RECORD_UPDATE        | Replace record field with the HTTP Response body | jmesPath             | Field to replace with the HTTP Response |
| RECORD_TERMINATE     | Don't forward record to the next step            |                      |                                         |
| RECORD_ROUTE         | Route record to specific topics                  | topics               | List of topics to route the record to   |

## Configure dynamic path

It's possible to configure the path dynamically using placeholders.  
The value correspondent to the placeholder need to be available inside the record.

Example:
```yaml
    config:
      api: my-dummy-api
      path: /my/path/${value.myDynamic.field}
      topic: error
```

So record value needs to look like this:
```json
{
    "myDynamic": {
        "field": 1234
    }
}
```

If the value is not there, the call will fail, and the record will be routed to the error topic.

## HTTP response errors

If in an HTTP call an error occurs, it will be saved as a header on the record.
