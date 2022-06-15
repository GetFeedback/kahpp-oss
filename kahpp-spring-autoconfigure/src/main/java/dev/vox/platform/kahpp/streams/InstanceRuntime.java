package dev.vox.platform.kahpp.streams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import dev.vox.platform.kahpp.configuration.Step;
import dev.vox.platform.kahpp.configuration.http.client.exception.RequestException;
import dev.vox.platform.kahpp.streams.Instance.Config;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeDeserializer;
import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.support.serializer.JsonSerializer;

public final class InstanceRuntime {
  private static InstanceRuntime INSTANCE_RUNTIME;
  private static Clock CLOCK;

  private final transient String group;
  private final transient String name;
  private final transient Integer version;

  private InstanceRuntime(Config instanceConfiguration) {
    this.group = instanceConfiguration.getGroup();
    this.name = instanceConfiguration.getName();
    this.version = instanceConfiguration.getVersion();
  }

  @SuppressWarnings("UseTimeInScope")
  public static void init(Config instanceConfiguration) {
    init(instanceConfiguration, Clock.system(ZoneId.systemDefault()));
  }

  public static void init(Config instanceConfiguration, Clock clock) {
    if (isCreated()) {
      throw new RuntimeException(
          "This object can be created only once. Use InstanceRuntime::get to get a instance.");
    }

    INSTANCE_RUNTIME = new InstanceRuntime(instanceConfiguration);
    InstanceRuntime.CLOCK = clock;
  }

  public static InstanceRuntime get() {
    if (!isCreated()) {
      throw new RuntimeException(
          "This object has to be created before using this method. Use InstanceRuntime::init");
    }

    return INSTANCE_RUNTIME;
  }

  @SuppressWarnings("PMD.NullAssignment")
  public static void close() {
    INSTANCE_RUNTIME = null;
  }

  public static boolean isCreated() {
    return INSTANCE_RUNTIME != null;
  }

  public static class HeaderHelper {

    private static final ObjectMapper OBJECT_MAPPER =
        new ObjectMapper()
            .registerModule(new Jdk8Module())
            .addMixIn(Exception.class, ExceptionMixIn.class)
            .addMixIn(RequestException.class, RequestExceptionMixIn.class);

    private static final JsonSerializer<Object> serializer = new JsonSerializer<>();
    private static final JsonNodeDeserializer deserializer = new JsonNodeDeserializer();

    public static org.apache.kafka.common.header.Header forSuccess(Step step) {
      InstanceRuntime instanceRuntime = InstanceRuntime.get();

      Header header = Header.fromSuccess(instanceRuntime.version);
      JsonNode jsonNode = OBJECT_MAPPER.convertValue(header, JsonNode.class);

      return new RecordHeader(
          getHeaderKey(instanceRuntime.group, instanceRuntime.name, step),
          serializer.serialize("header", jsonNode));
    }

    public static org.apache.kafka.common.header.Header forError(Step step, Throwable exception) {
      InstanceRuntime instanceRuntime = InstanceRuntime.get();

      Header header = Header.fromError(instanceRuntime.version, exception);
      JsonNode jsonNode = OBJECT_MAPPER.convertValue(header, JsonNode.class);

      return new RecordHeader(
          getHeaderKey(instanceRuntime.group, instanceRuntime.name, step),
          serializer.serialize("header", jsonNode));
    }

    public static org.apache.kafka.common.header.Header getStepHeader(Step step, Headers headers) {
      InstanceRuntime instanceRuntime = InstanceRuntime.get();
      return headers.lastHeader(getHeaderKey(instanceRuntime.group, instanceRuntime.name, step));
    }

    public static boolean isSuccessStepHeader(Step step, Headers headers) {
      org.apache.kafka.common.header.Header stepHeader = getStepHeader(step, headers);
      if (stepHeader == null) {
        return false;
      }
      Object header = deserializer.deserialize("header", stepHeader.value());
      // In the future we might also need to check if the version is the same
      return OBJECT_MAPPER.convertValue(header, Header.class).isSuccess();
    }

    private static String getHeaderKey(String group, String name, Step step) {
      return String.format("kahpp.%s.%s.%s", group, name, step.getName());
    }

    @JsonIgnoreProperties("stackTrace")
    private static class ExceptionMixIn {}

    @JsonIgnoreProperties({"request", "stackTrace"})
    private static class RequestExceptionMixIn {}
  }

  private static class Header {
    private final Long timestamp;
    private final Integer version;
    private final Status status;
    private final transient Map<String, Object> context = new HashMap<>();

    @SuppressWarnings("PMD.NullAssignment")
    public Header() {
      this.version = null;
      this.status = null;
      this.timestamp = null;
    }

    private Header(Integer version, Status status) {
      this.version = version;
      this.status = status;
      this.timestamp = CLOCK.instant().toEpochMilli();
    }

    static Header fromSuccess(Integer version) {
      return new Header(version, Status.SUCCESS);
    }

    static Header fromError(Integer version, Throwable exception) {
      Header header = new Header(version, Status.ERROR_UNKNOWN);
      header.context.put("exception", exception);
      return header;
    }

    boolean isSuccess() {
      return Status.SUCCESS.equals(this.getStatus());
    }

    @JsonProperty
    public Long getTimestamp() {
      return timestamp;
    }

    @JsonProperty
    public Integer getVersion() {
      return version;
    }

    @JsonProperty
    public Status getStatus() {
      return status;
    }

    public Optional<Map<String, Object>> getContext() {
      return Optional.ofNullable(context);
    }

    // all errors will be considered UNKNOWN for now once we don't have an error contract yet
    private enum Status {
      SUCCESS,
      ERROR_UNKNOWN,
    }
  }
}
