package dev.vox.platform.kahpp.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TransformRecord implements RecordAction {
  private final transient JsonNode dataSource;
  private final transient List<Mutation> mutations;

  private TransformRecord(JsonNode dataSource, List<Mutation> mutations) {
    this.dataSource = dataSource;
    this.mutations = mutations;
  }

  public static TransformRecord replacePath(String value, String jmesTo) {
    return replacePath(TextNode.valueOf(value), jmesTo);
  }

  public static TransformRecord replacePath(JsonNode dataSource, String jmesTo) {
    return new TransformRecord(dataSource, List.of(JmesPathMutation.pair("@", jmesTo)));
  }

  public static TransformRecord withMutations(JsonNode dataSource, List<Mutation> mutations) {
    return new TransformRecord(dataSource, mutations);
  }

  public static TransformRecord withMutation(JsonNode dataSource, Mutation mutation) {
    return new TransformRecord(dataSource, List.of(mutation));
  }

  public static TransformRecord replacePaths(JsonNode dataSource, List<Mutation> mutations) {
    return new TransformRecord(dataSource, mutations);
  }

  public static TransformRecord replacePath(JsonNode dataSource, String jmesFrom, String jmesTo) {
    return new TransformRecord(dataSource, List.of(JmesPathMutation.pair(jmesFrom, jmesTo)));
  }

  public static TransformRecord noTransformation() {
    return new TransformRecord(null, List.of());
  }

  public JsonNode getDataSource() {
    return dataSource;
  }

  public List<Mutation> getMutations() {
    return Collections.unmodifiableList(mutations);
  }

  @Override
  public boolean shouldForward() {
    return true;
  }

  public static class JmesPathMutation implements Mutation {
    private final transient String jmesFrom;

    // Always with the record schema .headers.bla (root `.` is our Record obj)
    private final transient String jmesTo;

    public static JmesPathMutation pair(String jmesFrom, String jmesTo) {
      return new JmesPathMutation(jmesFrom, jmesTo);
    }

    private JmesPathMutation(String jmesFrom, String jmesTo) {
      this.jmesFrom = jmesFrom;
      this.jmesTo = jmesTo;
    }

    public String getJmesFrom() {
      return jmesFrom;
    }

    public String getJmesTo() {
      return jmesTo;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof JmesPathMutation)) return false;

      JmesPathMutation mutation = (JmesPathMutation) o;

      if (!getJmesFrom().equals(mutation.getJmesFrom())) return false;
      return getJmesTo().equals(mutation.getJmesTo());
    }

    @Override
    public int hashCode() {
      return Objects.hash(jmesFrom, jmesTo);
    }
  }

  public static class RemoveFieldMutation implements Mutation {
    private final transient String field;

    public static RemoveFieldMutation field(String field) {
      return new RemoveFieldMutation(field);
    }

    private RemoveFieldMutation(String field) {
      this.field = field;
    }

    public String getField() {
      return field;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RemoveFieldMutation)) return false;

      RemoveFieldMutation that = (RemoveFieldMutation) o;

      return getField().equals(that.getField());
    }

    @Override
    public int hashCode() {
      return getField().hashCode();
    }
  }

  public interface Mutation {}
}
