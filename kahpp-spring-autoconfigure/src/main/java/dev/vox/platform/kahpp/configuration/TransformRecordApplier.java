package dev.vox.platform.kahpp.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;

public class TransformRecordApplier {

  private TransformRecordApplier() {}

  /*
   * todo: move this to external bean
   *  also improve the jmesRuntime
   */
  public static KaHPPRecord apply(
      JacksonRuntime jacksonRuntime,
      final KaHPPRecord record,
      final TransformRecord transformRecord) {
    KaHPPRecord mutatedRecord = record;
    // todo: improve strategy to apply all mutations in the record
    for (TransformRecord.Mutation mutation : transformRecord.getMutations()) {

      if (mutation instanceof TransformRecord.JmesPathMutation) {
        TransformRecord.JmesPathMutation jmesPathMutation =
            (TransformRecord.JmesPathMutation) mutation;
        Expression<JsonNode> fromExpression =
            jacksonRuntime.compile(jmesPathMutation.getJmesFrom());

        mutatedRecord =
            mutatedRecord.applyJmesPathExpression(
                fromExpression, jmesPathMutation.getJmesTo(), transformRecord.getDataSource());

      } else if (mutation instanceof TransformRecord.RemoveFieldMutation) {
        TransformRecord.RemoveFieldMutation removeFieldMutation =
            (TransformRecord.RemoveFieldMutation) mutation;
        mutatedRecord = mutatedRecord.applyRemoveField(removeFieldMutation.getField());
      }
    }

    return mutatedRecord;
  }
}
