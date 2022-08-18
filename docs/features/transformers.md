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

## Split Value
Split an array into values.

```yaml
    - name: splitFooArray
      type: dev.vox.platform.kahpp.configuration.transform.SplitValueTransform
      config:
        jmesPath: "value.fooArray"
```

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

```yaml
  - name: addNewField
    type: dev.vox.platform.kahpp.configuration.transform.InsertStaticFieldTransform
    config:
      field: newField
      value: foo
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
