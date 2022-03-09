package dev.vox.platform.kahpp.configuration;

public interface Step {
  String getName();

  default String getTypedName() {
    return String.format("%s.%s", getClass().getSimpleName(), getName());
  }
}
