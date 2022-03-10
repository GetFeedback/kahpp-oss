package dev.vox.platform.kahpp.unit.configuration.transform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vox.platform.kahpp.configuration.transform.ConfigurationToConvertZonedDateTimeFieldTransformStep;
import dev.vox.platform.kahpp.configuration.transform.ConfigurationToConvertZonedDateTimeFieldTransformStep.Formats;
import dev.vox.platform.kahpp.configuration.transform.ConvertZonedDateTimeFieldTransform;
import dev.vox.platform.kahpp.step.StepConfiguration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigurationToConvertZonedDateTimeFieldTransformStepTest {

  @Test
  void shouldNotConvertInvalidFormats() {
    Map<String, Object> config = Map.of("inputFormat", "fz", "outputFormat", "o");

    var newConfig = configure(config).getConfig();

    assertThat(newConfig).containsKeys("inputFormatter", "outputFormatter");
    assertThat(newConfig.get("inputFormatter")).isNull();
    assertThat(newConfig.get("outputFormatter")).isNull();
  }

  @Test
  void shouldSupportNativeFormatters() {
    Map<String, Object> config =
        Map.of("inputFormat", "RFC_1123_DATE_TIME", "outputFormat", "ISO_DATE_TIME");

    var newConfig = configure(config).getConfig();

    assertThat(newConfig).containsKeys("inputFormatter", "outputFormatter");
    assertThat(newConfig.get("inputFormatter")).isSameAs(DateTimeFormatter.RFC_1123_DATE_TIME);
    assertThat(newConfig.get("outputFormatter")).isSameAs(DateTimeFormatter.ISO_DATE_TIME);
  }

  @Test
  void shouldSupportCustomFormatters() {
    Map<String, Object> config =
        Map.of("inputFormat", "RFC_3339", "outputFormat", "RFC_3339_LENIENT_MS_FORMATTER");

    var newConfig = configure(config).getConfig();

    assertThat(newConfig).containsKeys("inputFormatter", "outputFormatter");
    assertThat(newConfig.get("inputFormatter")).isSameAs(Formats.RFC_3339);
    assertThat(newConfig.get("outputFormatter")).isSameAs(Formats.RFC_3339_LENIENT_MS_FORMATTER);
  }

  @Test
  void shouldSupportPatternFormatters() {
    Map<String, Object> config = Map.of("inputFormat", "y", "outputFormat", "D");

    var newConfig = configure(config).getConfig();

    assertThat(newConfig).containsKeys("inputFormatter", "outputFormatter");
    assertThat(newConfig.get("inputFormatter")).isInstanceOf(DateTimeFormatter.class);
    assertThat(newConfig.get("outputFormatter")).isInstanceOf(DateTimeFormatter.class);

    DateTimeFormatter inputFormatter = (DateTimeFormatter) newConfig.get("inputFormatter");
    DateTimeFormatter outputFormatter = (DateTimeFormatter) newConfig.get("outputFormatter");
    assertThat(inputFormatter.toString())
        .isEqualTo(
            new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR_OF_ERA)
                .toFormatter()
                .toString());
    assertThat(outputFormatter.toString())
        .isEqualTo(
            new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_YEAR)
                .toFormatter()
                .toString());
  }

  private StepConfiguration<ConvertZonedDateTimeFieldTransform> configure(
      Map<String, Object> config) {
    return new ConfigurationToConvertZonedDateTimeFieldTransformStep()
        .configure(
            new StepConfiguration<>(ConvertZonedDateTimeFieldTransform.class, "testStep", config),
            null);
  }
}
