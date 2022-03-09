package dev.vox.platform.kahpp.configuration.transform;

import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * This is a class used in the context of transforming KaHPP records.
 *
 * <p>To use this class as part of your instance's steps, you can add a new step as follows:
 *
 * <pre>
 * - name: -- name of the step --
 *   type: dev.vox.platform.kahpp.configuration.transform.DropFieldRecordTransform
 *   config:
 *     jmesPath: -- the JSON path to the property to be removed --
 * </pre>
 *
 * <p>The goal of this class is to remove an arbitrary property specified in the `jmesPath`.
 *
 * <p>Consider the following JSON Object:
 *
 * <pre>{ "value": { "type": "record", "settings": { "color": "green" } } }</pre>
 *
 * <p>When the specified `jmesPath` is `value.settings.color`, the class will remove the "color"
 * property, resulting in a JSON object as follows:
 *
 * <pre>{ "value": { "type": "record", "settings": {} } }</pre>
 */
public class DropFieldRecordTransform extends AbstractRecordTransform {

  @Pattern(regexp = "(key|value).*(\\.)\\w+")
  private final transient String jmesPath;

  public DropFieldRecordTransform(@NotBlank String name, Map<String, ?> config) {
    super(name, config);
    this.jmesPath = config.get("jmesPath").toString();
  }

  @Override
  public TransformRecord transform(
      JacksonRuntime jacksonRuntime, ProcessorContext context, KaHPPRecord record) {
    List<String> pieces = new ArrayList<>(Arrays.asList(jmesPath.split("\\.")));

    ObjectNode dataSource = (ObjectNode) record.getValue();
    String jmesToPath = pieces.get(0);

    if ("key".equals(jmesToPath)) {
      dataSource = (ObjectNode) record.getKey();
    }

    // Remove the root level of `key` or `value`
    pieces.remove(0);

    return TransformRecord.withMutation(
        dataSource, TransformRecord.RemoveFieldMutation.field(jmesPath));
  }
}
