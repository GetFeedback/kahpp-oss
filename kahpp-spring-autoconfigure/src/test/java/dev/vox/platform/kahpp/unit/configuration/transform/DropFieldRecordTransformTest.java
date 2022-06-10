package dev.vox.platform.kahpp.unit.configuration.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.TransformRecordApplier;
import dev.vox.platform.kahpp.configuration.transform.DropFieldRecordTransform;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.apache.kafka.streams.processor.MockProcessorContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@SpringBootTest(classes = {LocalValidatorFactoryBean.class})
class DropFieldRecordTransformTest {

  private static final String STEP_NAME = "dropField";
  private static final String JMESPATH_CONFIG_NAME = "jmesPath";

  @Autowired private transient Validator validator;

  private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final JacksonRuntime jacksonRuntime = new JacksonRuntime();
  private final MockProcessorContext mockProcessorContext = new MockProcessorContext();

  @Test
  void testJmesPathRegexAcceptOnlyKeyAndValueInFirstPiece() {
    DropFieldRecordTransform dropField =
        new DropFieldRecordTransform(STEP_NAME, Map.of(JMESPATH_CONFIG_NAME, "xablau.root"));

    Set<ConstraintViolation<DropFieldRecordTransform>> validate = validator.validate(dropField);

    assertThat(validate.size()).isEqualTo(1);
    assertThat(validate.iterator().next().getMessage())
        .isEqualTo("must match \"(key|value).*(\\.)\\w+\"");
  }

  @Test
  @SuppressWarnings("PMD.AvoidDuplicateLiterals")
  void testDropFieldReturnTransformRecordToTransformValueAtRootLevel() {
    DropFieldRecordTransform dropField =
        new DropFieldRecordTransform(STEP_NAME, Map.of(JMESPATH_CONFIG_NAME, "value.remove"));

    assertIsValid(dropField, validator);

    KaHPPRecord record = KaHPPRecord.build(createJsonNode(), createJsonNode(), 1584352842123L);

    TransformRecord transform = dropField.transform(jacksonRuntime, mockProcessorContext, record);
    TransformRecordApplier.apply(jacksonRuntime, record, transform);

    assertThat(transform.getMutations())
        .contains(TransformRecord.RemoveFieldMutation.field("value.remove"));
    assertThat(record.build().has("remove")).isFalse();
  }

  @Test
  void testDropFieldReturnTransformRecordToTransformValueAtNonRootLevel() {
    DropFieldRecordTransform dropField =
        new DropFieldRecordTransform(STEP_NAME, Map.of(JMESPATH_CONFIG_NAME, "value.nested.foo"));

    assertIsValid(dropField, validator);

    KaHPPRecord record = KaHPPRecord.build(createJsonNode(), createJsonNode(), 1584352842123L);

    TransformRecord transform = dropField.transform(jacksonRuntime, mockProcessorContext, record);
    TransformRecordApplier.apply(jacksonRuntime, record, transform);

    assertThat(transform.getMutations())
        .contains(TransformRecord.RemoveFieldMutation.field("value.nested.foo"));
    JsonNode value = record.getValue();
    assertThat(value.has("nested")).isTrue();
    JsonNode nested = value.get("nested");
    assertThat(nested).isNotNull();
    assertThat(nested.has("foo")).isFalse();
  }

  @Test
  void testDropFieldReturnTransformRecordToTransformKey() {
    DropFieldRecordTransform dropField =
        new DropFieldRecordTransform(STEP_NAME, Map.of(JMESPATH_CONFIG_NAME, "key.remove"));

    assertIsValid(dropField, validator);

    KaHPPRecord record = KaHPPRecord.build(createJsonNode(), createJsonNode(), 1584352842123L);

    TransformRecord transform = dropField.transform(jacksonRuntime, mockProcessorContext, record);
    TransformRecordApplier.apply(jacksonRuntime, record, transform);

    assertThat(transform.getMutations())
        .contains(TransformRecord.RemoveFieldMutation.field("key.remove"));
    assertThat(record.build().has("remove")).isFalse();
  }

  private static void assertIsValid(DropFieldRecordTransform dropField, Validator validator) {
    Set<ConstraintViolation<DropFieldRecordTransform>> validate = validator.validate(dropField);
    if (!validate.isEmpty()) {
      fail("Object validation failed: " + validate.toString());
    }
  }

  private static ObjectNode createJsonNode() {
    ObjectNode node = OBJECT_MAPPER.createObjectNode();
    node.put("remove", "nooo");
    node.put("stay", "yes");
    node.set("nested", OBJECT_MAPPER.createObjectNode().put("foo", "remove"));
    return node;
  }
}
