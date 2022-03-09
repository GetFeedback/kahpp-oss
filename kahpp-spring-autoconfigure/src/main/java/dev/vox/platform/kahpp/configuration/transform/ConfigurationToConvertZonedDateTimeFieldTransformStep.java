package dev.vox.platform.kahpp.configuration.transform;

import dev.vox.platform.kahpp.step.ConfigurationToStep;
import dev.vox.platform.kahpp.step.StepConfiguration;
import dev.vox.platform.kahpp.streams.Instance;
import java.lang.reflect.Field;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(400)
public class ConfigurationToConvertZonedDateTimeFieldTransformStep
    implements ConfigurationToStep<ConvertZonedDateTimeFieldTransform> {

  @Override
  public StepConfiguration<ConvertZonedDateTimeFieldTransform> configure(
      StepConfiguration<ConvertZonedDateTimeFieldTransform> stepConfiguration,
      Instance.ConfigBuilder configBuilder) {

    var config = getConfigAsHashMap(stepConfiguration);

    if (config.containsKey(ConvertZonedDateTimeFieldTransform.FIELD_INPUT_FORMAT)) {
      Object inputFormat = config.get(ConvertZonedDateTimeFieldTransform.FIELD_INPUT_FORMAT);
      if (inputFormat instanceof String) {
        config.put(
            ConvertZonedDateTimeFieldTransform.INTERNAL_FIELD_INPUT_FORMAT,
            createFormatter((String) inputFormat));
      }
    }

    if (config.containsKey(ConvertZonedDateTimeFieldTransform.FIELD_OUTPUT_FORMAT)) {
      Object inputFormat = config.get(ConvertZonedDateTimeFieldTransform.FIELD_OUTPUT_FORMAT);
      if (inputFormat instanceof String) {
        config.put(
            ConvertZonedDateTimeFieldTransform.INTERNAL_FIELD_OUTPUT_FORMAT,
            createFormatter((String) inputFormat));
      }
    }

    return stepConfiguration.newConfig(config);
  }

  private DateTimeFormatter createFormatter(String inputFormat) {
    var customFormats = new Formats();
    var nativeFormats = DateTimeFormatter.ISO_DATE_TIME;

    return getFormatterFromStaticField(inputFormat, customFormats)
        .or(() -> getFormatterFromStaticField(inputFormat, nativeFormats))
        .or(
            () -> {
              try {
                return Optional.of(DateTimeFormatter.ofPattern(inputFormat));
              } catch (IllegalArgumentException e) {
                return Optional.empty();
              }
            })
        .orElse(null);
  }

  private static Optional<DateTimeFormatter> getFormatterFromStaticField(
      String inputFormat, Object fakeInstance) {
    try {
      Field declaredField = fakeInstance.getClass().getDeclaredField(inputFormat);
      // We need a real instance to be able to fetch the field dynamically
      return Optional.of((DateTimeFormatter) declaredField.get(fakeInstance));
    } catch (NoSuchFieldException | IllegalAccessException | ClassCastException ignored) {
      return Optional.empty();
    }
  }

  @Override
  public Class<ConvertZonedDateTimeFieldTransform> supportsType() {
    return ConvertZonedDateTimeFieldTransform.class;
  }

  public static class Formats {
    public static final DateTimeFormatter RFC_3339 =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static final DateTimeFormatter RFC_3339_LENIENT_MS_FORMATTER =
        new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 9, true)
            .optionalStart()
            .appendZoneOrOffsetId()
            .optionalEnd()
            .optionalStart()
            .appendPattern("Z")
            .optionalEnd()
            .toFormatter();
  }
}
