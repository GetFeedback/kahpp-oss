package dev.vox.platform.kahpp.unit.configuration.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import dev.vox.platform.kahpp.configuration.RecordAction;
import dev.vox.platform.kahpp.configuration.TransformRecord;
import dev.vox.platform.kahpp.configuration.conditional.PathCondition;
import dev.vox.platform.kahpp.configuration.http.AbstractHttpCall;
import dev.vox.platform.kahpp.streams.KaHPPRecord;
import io.burt.jmespath.jackson.JacksonRuntime;
import io.vavr.control.Either;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpCallConditionalTest {

  @Test
  void shouldReturnTheConfiguredCondition() {
    final JacksonRuntime runtime = new JacksonRuntime();
    final HttpCallMockForTests httpCall =
        new HttpCallMockForTests(
            Map.of(
                "condition", new PathCondition("value==`true`", runtime.compile("value==`true`"))));

    assertThat(httpCall.condition()).isInstanceOf(PathCondition.class);
    assertThat(
            httpCall
                .condition()
                .test(
                    KaHPPRecord.build(
                        NullNode.getInstance(), BooleanNode.valueOf(true), 1584352842123L)))
        .isTrue();
    assertThat(
            httpCall
                .condition()
                .test(
                    KaHPPRecord.build(
                        NullNode.getInstance(), BooleanNode.valueOf(false), 1584352842123L)))
        .isFalse();
  }

  private static class HttpCallMockForTests extends AbstractHttpCall {
    public HttpCallMockForTests(Map<String, ?> config) {
      super("mockStep", config);
    }

    @Override
    public Either<Throwable, RecordAction> call(KaHPPRecord record) {
      return Either.right(TransformRecord.noTransformation());
    }
  }
}
