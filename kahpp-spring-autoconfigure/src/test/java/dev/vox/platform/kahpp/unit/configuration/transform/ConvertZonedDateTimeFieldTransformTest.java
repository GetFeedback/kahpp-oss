package dev.vox.platform.kahpp.unit.configuration.transform;

import static dev.vox.platform.kahpp.test.instance.InstanceTestConfiguration.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.transform.ConfigurationToConvertZonedDateTimeFieldTransformStep.Formats;
import dev.vox.platform.kahpp.configuration.transform.ConvertZonedDateTimeFieldTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConvertZonedDateTimeFieldTransformTest {

  private static final JacksonRuntime JACKSON_RUNTIME = new JacksonRuntime();
  private static final MockProcessorContext PROCESSOR_CONTEXT = new MockProcessorContext();

  public static final ConvertZonedDateTimeFieldTransform defaultTransform =
      new ConvertZonedDateTimeFieldTransform(
          "test",
          Map.of(
              "field", "value.date",
              "inputFormatter", Formats.RFC_3339_LENIENT_MS_FORMATTER,
              "outputFormatter", Formats.RFC_3339));

  @Test
  void assertCanConvertRFC3339Field() {
    String input = "2019-02-08T16:03:18+0000";

    TransformRecord transformRecord = transformRecord(defaultTransform, input);

    assertThat(transformRecord.getDataSource().asText()).isEqualTo("2019-02-08T16:03:18.000+0000");
    var mutations = transformRecord.getMutations();
    assertThat(mutations).hasSize(1);
    assertThat(mutations).contains(TransformRecord.JmesPathMutation.pair("@", "value.date"));
  }

  @Test
  void assertCanConvertOtherFormats() {
    ConvertZonedDateTimeFieldTransform transform =
        new ConvertZonedDateTimeFieldTransform(
            "test",
            Map.of(
                "field",
                "value.date",
                "inputFormatter",
                DateTimeFormatter.RFC_1123_DATE_TIME,
                "outputFormatter",
                Formats.RFC_3339));

    TransformRecord transformRecord = transformRecord(transform, "Fri, 26 Mar 2021 11:05:30 GMT");

    assertThat(transformRecord.getDataSource().asText()).isEqualTo("2021-03-26T11:05:30.000+0000");
    var mutations = transformRecord.getMutations();
    assertThat(mutations).hasSize(1);
    assertThat(mutations).contains(TransformRecord.JmesPathMutation.pair("@", "value.date"));
  }

  @Test
  void assertNoTransformationIsDoneWhenFieldIsInvalid() {
    ConvertZonedDateTimeFieldTransform transform =
        new ConvertZonedDateTimeFieldTransform(
            "test",
            Map.of(
                "field",
                "value.fakeDate",
                "inputFormatter",
                DateTimeFormatter.RFC_1123_DATE_TIME,
                "outputFormatter",
                Formats.RFC_3339));

    TransformRecord transformRecord = transformRecord(transform, "d");

    var mutations = transformRecord.getMutations();
    assertThat(mutations).hasSize(0);
  }

  @Test
  void assertNoTransformationIsDoneWhenFieldIsEmpty() {
    TransformRecord transformRecord = transformRecord(defaultTransform, "");

    var mutations = transformRecord.getMutations();
    assertThat(mutations).hasSize(0);
  }

  @Test
  void assertNoTransformationIsDoneWhenFormatIsInvalid() {
    TransformRecord transformRecord = transformRecord(defaultTransform, "20210326");

    var mutations = transformRecord.getMutations();
    assertThat(mutations).hasSize(0);
  }

  @Test
  void assertRFC3339ImpliesMilliseconds() {
    ZonedDateTime withMs =
        ZonedDateTime.parse("2019-02-08T16:03:18.000+0000", Formats.RFC_3339_LENIENT_MS_FORMATTER);
    ZonedDateTime withoutMs =
        ZonedDateTime.parse("2019-02-08T16:03:18+0000", Formats.RFC_3339_LENIENT_MS_FORMATTER);

    assertThat(withMs).isEqualTo(withoutMs);
    assertThat(withoutMs.format(Formats.RFC_3339)).isEqualTo("2019-02-08T16:03:18.000+0000");
    assertThat(withMs.format(Formats.RFC_3339)).isEqualTo(withoutMs.format(Formats.RFC_3339));
  }

  @Test
  void assertRFC3339PreservesMillisecondsWhenPresent() {
    ZonedDateTime withMs =
        ZonedDateTime.parse("2019-02-08T16:03:18.123+0000", Formats.RFC_3339_LENIENT_MS_FORMATTER);

    assertThat(withMs.format(Formats.RFC_3339)).isEqualTo("2019-02-08T16:03:18.123+0000");
    assertThat(withMs.getNano()).isEqualTo(123000000);
  }

  @ParameterizedTest
  @CsvSource({
    "2021-03-29T10:01:14+00:00,2021-03-29T10:01:14.000+0000",
    "2021-03-29T10:01:14-01:00,2021-03-29T10:01:14.000-0100",
    "2021-03-29T10:01:14UTC,2021-03-29T10:01:14.000+0000",
    "2021-03-29T10:01:14GMT+01:30,2021-03-29T10:01:14.000+0130",
    "2021-08-02T12:00:00+08:00,2021-08-02T12:00:00.000+0800",
    "2021-08-02T12:00:00.000+0000,2021-08-02T12:00:00.000+0000",
    "2021-08-02T12:00:00+0000,2021-08-02T12:00:00.000+0000",
    "2021-08-02T12:00:00.555+0000,2021-08-02T12:00:00.555+0000",
    "2021-08-02T12:00:00.5555555+0000,2021-08-02T12:00:00.555+0000",
    "2021-08-02T12:00:00.5+0000,2021-08-02T12:00:00.500+0000",
    "2021-08-02T12:00:00.+0000,2021-08-02T12:00:00.000+0000",
    "2021-08-02T12:00:00.000+1000,2021-08-02T12:00:00.000+1000",
    "2021-08-02T12:00:00.342+1111,2021-08-02T12:00:00.342+1111",
    "2021-08-02T12:00:00.456-1111,2021-08-02T12:00:00.456-1111",
  })
  void assertRFC3339Conversions(String input, String expectedOutput) {
    ZonedDateTime withMs = ZonedDateTime.parse(input, Formats.RFC_3339_LENIENT_MS_FORMATTER);

    assertThat(withMs.format(Formats.RFC_3339)).isEqualTo(expectedOutput);
  }

  private TransformRecord transformRecord(
      ConvertZonedDateTimeFieldTransform transform, String input) {
    final JsonNode value = MAPPER.createObjectNode().put("date", input);
    KaHPPRecord record = KaHPPRecord.build(NullNode.getInstance(), value, 1584352842123L);

    return transform.transform(JACKSON_RUNTIME, PROCESSOR_CONTEXT, record);
  }
}
