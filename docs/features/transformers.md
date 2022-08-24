# Transformers

These _steps_ make it possible to re-shape records on your need.

## Move Field
Permits to move a field.

```yaml
  - name: moveBarToNewBar
    type: dev.vox.platform.kahpp.configuration.transform.MoveFieldRecordTransform
    config:
      from: value.bar
      to: value.newBar
```

## Copy Field
Permits to copy a field.

```yaml
  - name: copyKeyToValue
    type: dev.vox.platform.kahpp.configuration.transform.CopyFieldRecordTransform
    config:
      from: key
      to: value.key
```

## Drop Field
Permits to remove a field.

```yaml
  - name: removeThisField
    type: dev.vox.platform.kahpp.configuration.transform.DropFieldRecordTransform
    config:
      jmesPath: value.unlike
```

## Convert ZonedDateTime Field
Converts a date from one format to another.

```yaml
  - name: ensureCreatedAtDateIsRFC3339
    type: dev.vox.platform.kahpp.configuration.transform.ConvertZonedDateTimeFieldTransform
    config:
      field: value.createdAt
      inputFormat: RFC_3339_LENIENT_MS_FORMATTER
      outputFormat: RFC_3339
```

## Extract Field
Pulls a field out of a complex value and replaces the entire value with the extracted field.

```yaml
  - name: extractId
    type: dev.vox.platform.kahpp.configuration.transform.ExtractFieldValueTransform
    config:
      field: id
```

## Split array
Split an array into multiple records.

Let's take as example this record:
```
{
    "customer": "Paolo",
    "orders": [
        {
            "type": "food",
            "product": "pizza"
        },
        {
            "type": "drink",
            "product": "cocacola"
        }
    ]
}
```

We would like to send the order for food and drink in two different places (for example to two different topics).  

So the expected result is to have two records.  

One for the food:
```
{
    "customer": "Paolo",
    "order": {
        "type": "food",
        "product": "pizza"
    }
}
```

Another for the drink:
```
{
    "customer": "Paolo",
    "order": {
        "type": "food",
        "product": "pizza"
    }
}
```

So, our step configuration will look like:
```yaml
    - name: splitFooArray
      type: dev.vox.platform.kahpp.configuration.transform.SplitValueTransform
      config:
        jmesPath: "value.orders"
        to: "value.order" # Optional field
```

### Optional configurations

| name | default | description                                                                                                                                                                                                                                                                            |
|------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| to   | ""      | Defines where to put an extracted array's object as a nested field into the original parent object. If the 'to' field is not configured, the extracted object will go on the 'value' field by default. The existing parent's field under the same 'to' path is going to be overridden. |


## Copy Timestamp To Value
Copy timestamp of record into value.

```yaml
  - name: timestampToValue
    type: dev.vox.platform.kahpp.configuration.transform.TimestampToValueTransform
    config:
      field: publication_date
```

## Unwrap Value

Unwraps the content of one field to root value.
```yaml
  - name: payloadUnwrap
    type: dev.vox.platform.kahpp.configuration.transform.UnwrapValueTransform
    config:
      field: payload
```

## Wrap Value

Wraps the current content in a single field.
```yaml
  - name: wrapPayload
    type: dev.vox.platform.kahpp.configuration.transform.WrapValueTransform
    config:
      field: payload
```

## Insert Static Value

Insert a static value in a chosen field of the value object; if the field exists, the action will be skipped, and eventually is possible to configure the step with `overrideIfExists: true` to permit the override.

Let's take as example this record:

```yaml
{
    "name": "Paolo",
    "email": "paolo@him.com"
}
```

We want to add a new field `type`, so our record will look like this:

```yaml
{
    "name": "Paolo",
    "email": "paolo@him.com",
    "type": "customer"
}
```

So, our step configuration will look like this:

```yaml
  - name: addNewField
    type: dev.vox.platform.kahpp.configuration.transform.InsertStaticFieldTransform
    config:
      field: type
      value: customer
```

### Insert JSON Value 

Eventually, we can also add a static JSON value.
```yaml
  - name: addNewJsonField
    type: dev.vox.platform.kahpp.configuration.transform.InsertStaticFieldTransform
    config:
      field: newJsonField
      value: '[{"foo":"bar"}]'
      format: json
 
```

### Override an existing field

It's also possible to override an existent field.

```yaml
  - name: addNewJsonField
    type: dev.vox.platform.kahpp.configuration.transform.InsertStaticFieldTransform
    config:
      field: email
      value: '*****'
      overrideIfExists: true
```

## Conditional 

All transformer steps can be triggered conditionally using the parameter `condition`.  
If the expression is true, the action will be started.

Example:
```yaml
  - name: moveBarToNewBar
    type: dev.vox.platform.kahpp.configuration.transform.MoveFieldRecordTransform
    config:
      condition: "value.bar != null"
      from: value.bar
      to: value.newBar
```
