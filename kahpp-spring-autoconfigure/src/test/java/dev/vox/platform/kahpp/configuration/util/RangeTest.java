package dev.vox.platform.kahpp.configuration.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RangeTest {

  @Test
  public void testContainsTrue() {
    var range = new Range(10, 20);
    assertTrue(range.contains(10));
    assertTrue(range.contains(15));
    assertTrue(range.contains(20));
  }

  @Test
  public void testContainsFalse() {
    var range = new Range(10, 20);
    assertFalse(range.contains(9));
    assertFalse(range.contains(21));
  }
}
