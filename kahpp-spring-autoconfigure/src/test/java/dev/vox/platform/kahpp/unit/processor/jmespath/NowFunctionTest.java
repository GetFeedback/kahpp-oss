package dev.vox.platform.kahpp.unit.processor.jmespath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vox.platform.kahpp.processor.jmespath.NowFunction;
import io.burt.jmespath.function.FunctionRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class NowFunctionTest extends JmespathFunctionTest {
  private static final Clock clock =
      Clock.fixed(Instant.parse("2021-10-05T10:00:00Z"), ZoneId.systemDefault());

  @Test
  void evaluateNowWithPlus() {
    assertEquals(
        parse(Instant.parse("2021-10-06T10:00:00Z").toEpochMilli()), evaluate("now('+PT86400S')"));
    assertEquals(
        parse(Instant.parse("2021-10-05T13:00:00Z").toEpochMilli()), evaluate("now('+PT3H')"));
    assertEquals(
        parse(Instant.parse("2021-10-10T10:00:00Z").toEpochMilli()), evaluate("now('+P5D')"));
    assertEquals(
        parse(Instant.parse("2021-10-05T10:00:01Z").toEpochMilli()), evaluate("now('+PT1S')"));
    assertEquals(
        parse(Instant.parse("2021-10-05T10:03:00Z").toEpochMilli()), evaluate("now('+PT3M')"));
    assertEquals(
        parse(Instant.parse("2021-10-06T10:00:00Z").toEpochMilli()), evaluate("now('+P1D')"));
  }

  @Test
  void evaluateNowWithMinus() {
    assertEquals(
        parse(Instant.parse("2021-10-04T10:00:00Z").toEpochMilli()), evaluate("now('-P1D')"));
    assertEquals(
        parse(Instant.parse("2021-09-25T10:00:00Z").toEpochMilli()), evaluate("now('-P10D')"));
    assertEquals(
        parse(Instant.parse("2021-10-05T09:59:59Z").toEpochMilli()), evaluate("now('-PT1S')"));
  }

  @Override
  protected FunctionRegistry functionRegistry() {
    return FunctionRegistry.defaultRegistry().extend(new NowFunction(clock));
  }
}
