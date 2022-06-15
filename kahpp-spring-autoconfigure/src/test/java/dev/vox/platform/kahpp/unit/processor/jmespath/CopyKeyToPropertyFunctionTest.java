package dev.vox.platform.kahpp.unit.processor.jmespath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.vox.platform.kahpp.processor.jmespath.CopyKeyToPropertyFunction;
import io.burt.jmespath.function.ArgumentTypeException;
import io.burt.jmespath.function.FunctionRegistry;
import org.junit.jupiter.api.Test;

class CopyKeyToPropertyFunctionTest extends JmespathFunctionTest {
  @Test
  void copyKeyToProperty() {
    assertEquals(
        parse("{\"foo\":{\"key\":\"foo\",\"bar\":\"baz\",\"hello\":\"world\"}}"),
        evaluate(
            "copy_key_to_property(`{\"foo\":{\"bar\":\"baz\",\"hello\":\"world\"}}`, `\"key\"`)"));
  }

  @Test
  void copyKeyToPropertyForEmptyObject() {
    assertEquals(parse("{}"), evaluate("copy_key_to_property(`{}`, `\"key\"`)"));
  }

  @Test
  void copyKeyToPropertyForMultipleObjects() {
    assertEquals(
        parse(
            "{\"foo\":{\"id\":\"foo\",\"hello\":\"world\"},\"bar\":{\"id\":\"bar\",\"hello\":\"world\"},\"baz\":{\"id\":\"baz\",\"hello\":\"world\"}}"),
        evaluate(
            "copy_key_to_property(`{\"foo\":{\"hello\":\"world\"},\"bar\":{\"hello\":\"world\"},\"baz\":{\"hello\":\"world\"}}`, `\"id\"`)"));
  }

  @Test
  void copyKeyToPropertyThrowsExceptionWhenKeyAlreadyInUse() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            evaluate(
                "copy_key_to_property(`{\"foo\":{\"bar\":\"baz\",\"hello\":\"world\"}}`, `\"bar\"`)"));
  }

  @Test
  void copyKeyToPropertyThrowsExceptionWhenFirstArgumentIsNotAnObject() {
    assertThrows(
        ArgumentTypeException.class, () -> evaluate("copy_key_to_property(`[]`, `\"foo\"`)"));
  }

  @Test
  void copyKeyToPropertyThrowsExceptionWhenSecondArgumentIsNotAString() {
    assertThrows(ArgumentTypeException.class, () -> evaluate("copy_key_to_property(`{}`, `[]`)"));
  }

  @Override
  protected FunctionRegistry functionRegistry() {
    return FunctionRegistry.defaultRegistry().extend(new CopyKeyToPropertyFunction());
  }
}
