package dev.vox.platform.kahpp.unit.processor.jmespath;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.vox.platform.kahpp.processor.jmespath.ArrayIntersectFunction;
import io.burt.jmespath.function.FunctionRegistry;
import org.junit.jupiter.api.Test;

class ArrayIntersectFunctionTest extends JmespathFunctionTest {
  @Test
  public void intersectWithEmptyListForA() {
    assertEquals(parse("[]"), evaluate("array_intersect(`[]`, `[\"red\"]`)"));
  }

  @Test
  public void intersectWithEmptyListForB() {
    assertEquals(parse("[]"), evaluate("array_intersect(`[\"red\"]`, `[]`)"));
  }

  @Test
  public void intersectWithOneMatch() {
    assertEquals(
        parse("[\"red\"]"), evaluate("array_intersect(`[\"red\", \"blue\"]`, `[\"red\"]`)"));
  }

  @Test
  public void intersectWithOneMatchInverse() {
    assertEquals(
        parse("[\"red\"]"), evaluate("array_intersect(`[\"red\"]`, `[\"red\", \"blue\"]`)"));
  }

  @Test
  public void intersectWithoutAnyOverlapInTheLists() {
    assertEquals(
        parse("[]"), evaluate("array_intersect(`[\"red\", \"blue\"]`, `[\"green\", \"black\"]`)"));
  }

  @Test
  public void intersectWithSameLists() {
    assertEquals(
        parse("[\"red\", \"blue\"]"),
        evaluate("array_intersect(`[\"red\", \"blue\"]`, `[\"red\", \"blue\"]`)"));
  }

  @Test
  public void intersectWithDuplicatesInListA() {
    assertEquals(
        parse("[\"red\"]"),
        evaluate("array_intersect(`[\"red\", \"red\", \"blue\"]`, `[\"red\"]`)"));
  }

  @Test
  public void intersectWithDuplicatesInListB() {
    assertEquals(
        parse("[\"red\"]"),
        evaluate("array_intersect(`[\"red\", \"blue\"]`, `[\"red\", \"red\"]`)"));
  }

  @Override
  protected FunctionRegistry functionRegistry() {
    return FunctionRegistry.defaultRegistry().extend(new ArrayIntersectFunction());
  }
}
