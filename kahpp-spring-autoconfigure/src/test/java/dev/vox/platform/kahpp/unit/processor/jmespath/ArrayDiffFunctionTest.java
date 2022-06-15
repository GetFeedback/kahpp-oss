package dev.vox.platform.kahpp.unit.processor.jmespath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vox.platform.kahpp.processor.jmespath.ArrayDiffFunction;
import io.burt.jmespath.function.FunctionRegistry;
import org.junit.jupiter.api.Test;

class ArrayDiffFunctionTest extends JmespathFunctionTest {
  @Test
  void diffWithEmptyListForA() {
    assertEquals(parse("[]"), evaluate("array_diff(`[]`, `[\"red\"]`)"));
  }

  @Test
  void diffWithEmptyListForB() {
    assertEquals(parse("[\"red\"]"), evaluate("array_diff(`[\"red\"]`, `[]`)"));
  }

  @Test
  void diffWithOneMatch() {
    assertEquals(parse("[\"blue\"]"), evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"red\"]`)"));
  }

  @Test
  void diffWithOneMatchInverse() {
    assertEquals(parse("[]"), evaluate("array_diff(`[\"red\"]`, `[\"red\", \"blue\"]`)"));
  }

  @Test
  void diffWithoutAnyOverlapInTheLists() {
    assertEquals(
        parse("[\"red\", \"blue\"]"),
        evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"green\", \"black\"]`)"));
  }

  @Test
  void diffWithSameLists() {
    assertEquals(parse("[]"), evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"red\", \"blue\"]`)"));
  }

  @Test
  void diffWithDuplicatesInListA() {
    assertEquals(
        parse("[\"red\"]"), evaluate("array_diff(`[\"red\", \"red\", \"blue\"]`, `[\"blue\"]`)"));
  }

  @Test
  void diffWithDuplicatesInListB() {
    assertEquals(
        parse("[\"red\"]"), evaluate("array_diff(`[\"red\", \"blue\"]`, `[\"blue\", \"blue\"]`)"));
  }

  @Override
  protected FunctionRegistry functionRegistry() {
    return FunctionRegistry.defaultRegistry().extend(new ArrayDiffFunction());
  }
}
