package dev.vox.platform.kahpp.configuration.util;

/** Closed range. */
public class Range {
  private final int lowerIncluding;
  private final int upperIncluding;

  public Range(int lowerIncluding, int upperIncluding) {
    this.lowerIncluding = lowerIncluding;
    this.upperIncluding = upperIncluding;
  }

  /** Returns true if value is within the bounds of this range. */
  public boolean contains(int val) {
    return lowerIncluding <= val && val <= upperIncluding;
  }
}
