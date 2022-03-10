package dev.vox.platform.kahpp.unit.processor.jmespath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vox.platform.kahpp.processor.jmespath.ArrayDiffFunction;
import io.burt.jmespath.function.FunctionRegistry;
import org.junit.jupiter.api.Test;

class ArrayDiffFunctionTest extends JmespathFunctionTest {
  @Test
  public void diffWithEmptyListForA() {
    assertEquals(parse("[]"), evaluate("array_diff(`[]`, `[\"red\"]`)"));
  }

  @Test
  public void diffWithEmptyListForB() {
    assertEquals(parse("[\"red\"]"), evaluate("array_diff(`[\"red\"]`, `[]`)"));
  }

  @Test
  public void diffWithOneMatch() {
    assertEquals(parse("[\"blue\"]"), evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"red\"]`)"));
  }

  @Test
  public void diffWithOneMatchInverse() {
    assertEquals(parse("[]"), evaluate("array_diff(`[\"red\"]`, `[\"red\", \"blue\"]`)"));
  }

  @Test
  public void diffWithoutAnyOverlapInTheLists() {
    assertEquals(
        parse("[\"red\", \"blue\"]"),
        evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"green\", \"black\"]`)"));
  }

  @Test
  public void diffWithSameLists() {
    assertEquals(parse("[]"), evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"red\", \"blue\"]`)"));
  }

  @Test
  public void diffWithDuplicatesInListA() {
    assertEquals(
        parse("[\"red\"]"), evaluate("array_diff(`[\"red\", \"red\", \"blue\"]`, `[\"blue\"]`)"));
  }

  @Test
  public void diffWithDuplicatesInListB() {
    assertEquals(
        parse("[\"red\"]"), evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"blue\", \"blue\"]`)"));
  }

  @Override
  protected FunctionRegistry functionRegistry() {
    return FunctionRegistry.defaultRegistry().extend(new ArrayDiffFunction());
  }
}
