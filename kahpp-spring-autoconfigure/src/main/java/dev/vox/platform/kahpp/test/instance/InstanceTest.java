package dev.vox.platform.kahpp.test.instance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vox.platform.kahpp.KafkaStreams;
import dev.vox.platform.kahpp.configuration.util.KafkaHeaderConverter;
import dev.vox.platform.kahpp.step.StepBuilder;
import dev.vox.platform.kahpp.streams.Instance;
import dev.vox.platform.kahpp.streams.InstanceRuntime;
import dev.vox.platform.kahpp.streams.StepBuilderConfiguration;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeDeserializer;
import dev.vox.platform.kahpp.streams.serialization.JsonNodeSerializer;
import dev.vox.platform.kahpp.streams.serialization.Serdes;
import dev.vox.platform.kahpp.test.instance.pact.PactConfigurationToHttpCallStep;
import dev.vox.platform.kahpp.test.instance.pact.PactMockServiceRegistry;
import dev.vox.platform.kahpp.test.instance.test.KaHPPTestRecord;
import dev.vox.platform.kahpp.test.instance.test.KaHPPTestScenario;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.EnabledIf;

/**
 * Fully functional KaHPP tests powered by TopologyTestDriver. They're not meant to run as regular
 * unit tests as they're suppose to have a real KaHPP configuration file. See the available
 * tests/tasks by running: `./gradlew tasks --group=kahpp`
 */
@SpringBootTest(
    classes = {
      Serdes.class,
      InstanceTestConfiguration.class,
      Instance.class,
      StepBuilderConfiguration.class,
      PactMockServiceRegistry.class,
      PactConfigurationToHttpCallStep.class,
      StepBuilder.class
    })
@TestPropertySource(
    properties = {
      "spring.config.location=classpath:/application-test.properties,classpath:/functional/${kahpp-test.instance}/kahpp-instance.yaml"
    })
@EnabledIf("#{ systemProperties['kahpp-test.instance'] != null }")
public class InstanceTest {

  @Value("${kahpp-test.instance}")
  private transient String functionalTestName;

  @Value("${kahpp-test.build-dir}")
  private transient String kahppBuildDir;

  @Autowired
  @Qualifier("instance")
  private transient Instance configuration;

  @Autowired private transient StepBuilderConfiguration.StepBuilderMap stepBuilderMap;

  @Autowired private transient PactMockServiceRegistry mockServiceRegistry;

  @Autowired
  @Qualifier("SerdeJsonNodeKey")
  private transient Serde<JsonNode> serdeKey;

  @Autowired
  @Qualifier("SerdeJsonNodeValue")
  private transient Serde<JsonNode> serdeValue;

  private transient Path pactsDir;

  private transient TopologyTestDriver testDriver;
  private transient TestInputTopic<JsonNode, JsonNode> sourceTopic;
  private final transient Map<String, TestOutputTopic<JsonNode, JsonNode>> outputTopics =
      new HashMap<>();
  private transient Topology topology;
  private transient KafkaStreams kahpp;

  private static transient ObjectMapper objectMapper = new ObjectMapper();
  private static transient KafkaHeaderConverter kafkaHeaderConverter =
      new KafkaHeaderConverter(objectMapper);

  @BeforeEach
  public void setUp() {
    this.pactsDir = Paths.get(kahppBuildDir, "pacts");

    Instance.Config config = configuration.getConfig();

    topology = new Topology();

    kahpp = new KafkaStreams(configuration, stepBuilderMap, serdeKey, serdeValue);

    // Re-instantiate in order to freeze the Clock
    InstanceRuntime.close();
    InstanceRuntime.init(config, InstanceTestConfiguration.CLOCK_FIXED);

    kahpp.getTopologyBuilder().configureTopology(topology);

    testDriver = new TopologyTestDriver(topology, kahpp.kStreamsConfigs().asProperties());

    sourceTopic =
        testDriver.createInputTopic(
            config.getTopics().getSource().getName(),
            serdeKey.serializer(),
            serdeValue.serializer(),
            InstanceTestConfiguration.CLOCK_FROZEN_INSTANT,
            InstanceTestConfiguration.CLOCK_TICK);

    var outputKeyDeserializer = new JsonNodeDeserializer();
    var outputValueDeserializer = new JsonNodeDeserializer();
    Map<String, ?> configMap =
        shouldDeserializeStrings(kahpp.kStreamsConfigs().asProperties())
            ? Map.of(JsonNodeDeserializer.JSON_DESERIALIZE_STRING_AS_TEXT_NODE, "true")
            : Map.of();
    outputKeyDeserializer.configure(configMap, true);
    outputValueDeserializer.configure(configMap, false);

    config
        .getTopics()
        .getSinkTopics()
        .forEach(
            (identifier, topic) -> {
              TestOutputTopic<JsonNode, JsonNode> outputTopic =
                  testDriver.createOutputTopic(
                      topic.getName(), outputKeyDeserializer, outputValueDeserializer);
              outputTopics.put(identifier, outputTopic);
            });
  }

  private boolean shouldDeserializeStrings(Properties kStreamsProperties) {
    return kStreamsProperties
        .getOrDefault(JsonNodeSerializer.JSON_SERIALIZE_TEXT_NODE_AS_STRING, "false")
        .equals("true");
  }

  @AfterEach
  public void tearDown() {
    testDriver.close();
    kahpp.close();
  }

  @Test
  @SuppressWarnings({"PMD.SystemPrintln"})
  public void printTopology() throws IOException {
    final TopologyDescription topologyDescription = topology.describe();

    Assertions.assertThat(topologyDescription).isNotNull();

    String topology = topologyDescription.toString();
    System.out.println(topology);

    Files.write(
        Paths.get(kahppBuildDir, "topology.txt"), topology.getBytes(Charset.defaultCharset()));
  }

  @Test
  public void loadsCorrectly() {
    Assertions.assertThat(this.configuration.getConfig().getSteps()).isNotEmpty();
    Assertions.assertThat(this.functionalTestName).isNotBlank();
  }

  @EnabledIf(
      "#{(T(java.lang.ClassLoader).getSystemResource('functional/${kahpp-test.instance}/scenarios') != null)"
          + " and "
          + "(T(java.nio.file.Files).exists(T(java.nio.file.Path).of(T(java.lang.ClassLoader).getSystemResource('functional/${kahpp-test.instance}/scenarios').toURI())))}")
  @Test
  public void functionalTest() throws IOException, URISyntaxException {
    List<KaHPPTestScenario> testScenarios = buildScenarios();
    Assertions.assertThat(testScenarios).hasSizeGreaterThanOrEqualTo(1);

    for (KaHPPTestScenario scenario : testScenarios) {
      mockServiceRegistry.setupInteractions(scenario);

      // Produce source Record
      sourceTopic.pipeInput(scenario.getSourceRecord());

      try {
        // Loop over every topic, fetch all produced records and verify they meet the expectations
        this.outputTopics
            .entrySet()
            .iterator()
            .forEachRemaining(
                (outputTopicEntry) -> verifyRecordsForSinkTopic(outputTopicEntry, scenario));
      } catch (AssertionError e) {
        // Summarizes all found records in the Topology for easier understanding of the
        // failed assertions.
        throw new AssertionError(enrichTestError(), e);
      }

      mockServiceRegistry.verifyInteractions(scenario);
    }

    mockServiceRegistry.generateAllPacts(configuration.getConfig().getApplicationID(), pactsDir);
  }

  protected String enrichTestError() throws JsonProcessingException {
    StringBuilder errorContext =
        new StringBuilder("Failed to assert TestRecord, more context below:");
    for (Map.Entry<String, TestOutputTopic<JsonNode, JsonNode>> entry :
        this.outputTopics.entrySet()) {
      String node = entry.getKey();
      TestOutputTopic<JsonNode, JsonNode> outputTopic = entry.getValue();
      List<TestRecord<JsonNode, JsonNode>> testRecords = outputTopic.readRecordsToList();
      List<KaHPPTestRecord> kaHPPTestRecords =
          testRecords.stream().map(KaHPPTestRecord::from).collect(Collectors.toList());

      // If you came here to understand why it's 0, this means that either:
      // - All the records were consumed and the assertion failed
      // - Simply there are no Records as they might have been terminated in a Step
      errorContext
          .append('\n')
          .append(
              String.format(
                  "[%s Remaining records found in sink '%s']", kaHPPTestRecords.size(), node));
      for (KaHPPTestRecord kaHPPTestRecord : kaHPPTestRecords) {
        String s = InstanceTestConfiguration.MAPPER.writeValueAsString(kaHPPTestRecord);
        errorContext.append("\n\t").append(s);
      }
    }

    errorContext.append('\n');
    return errorContext.toString();
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  protected List<KaHPPTestScenario> buildScenarios() throws IOException, URISyntaxException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    URL resource =
        classLoader.getResource(String.format("functional/%s/scenarios", this.functionalTestName));
    // Should skip the test and not fail
    Assertions.assertThat(resource).isNotNull();

    File scenariosDirectory = Paths.get(resource.toURI()).toFile();
    Assertions.assertThat(scenariosDirectory).isDirectory();
    List<Path> scenariosPath = getEachScenarioPath(scenariosDirectory.toPath());

    List<KaHPPTestScenario> testScenarios = new ArrayList<>();
    // Scenarios are monotonically increasing integers, i.e.: `1`, `2`, `3`, `4`...
    for (Path scenarioPath : scenariosPath) {
      Path kafkaFixturesPath = scenarioPath.resolve("kafka");

      // Find `source` fixture to produce to the input topic
      KaHPPTestRecord sourceRecord = readFixture(kafkaFixturesPath, "source.json");

      // Create a KaHPPTestScenario which holds all the data from a given scenario,
      // Topic fixtures, Http mocks, etc.
      KaHPPTestScenario testScenario = new KaHPPTestScenario(scenarioPath, sourceRecord);

      // Resolve directories for `sink` topics with record expectations
      List<Path> sinkFixtureDirectories = getSinkFixturesDirectoriesForScenario(kafkaFixturesPath);

      // Add all sink record expectations to the test scenario
      for (Path sinkFixtureDirectory : sinkFixtureDirectories) {
        List<Path> fixturePathsForSinkTopic = getFixtureFilePathsInDirectory(sinkFixtureDirectory);
        List<TestRecord<JsonNode, JsonNode>> testRecordsForSinkTopic =
            fixturePathsForSinkTopic.stream()
                .map(file -> readFixture(sinkFixtureDirectory, file.getFileName().toString()))
                .collect(Collectors.toList());
        testScenario.addExpectedRecords(
            sinkFixtureDirectory.getFileName().toString(), testRecordsForSinkTopic);
      }

      // Add all Api interactions to the scenario
      File apiInteractionsDirectory = new File(scenarioPath.toString(), "api");

      if (apiInteractionsDirectory.isDirectory()) {
        Map<String, Map<Path, JsonNode>> interactionsPerApi =
            getScenarioApiInteractions(apiInteractionsDirectory.toPath());
        interactionsPerApi.forEach(testScenario::addPactInteractions);
      }

      testScenarios.add(testScenario);
    }

    return testScenarios;
  }

  private Map<String, Map<Path, JsonNode>> getScenarioApiInteractions(Path apiInteractions)
      throws IOException {
    try (Stream<Path> walk = Files.walk(apiInteractions, 2)) {
      return walk.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".json"))
          .reduce(
              new HashMap<>(),
              (interactionsPerApiRaw, path) -> {
                String apiIdentifier = path.getParent().getFileName().toString();

                if (!interactionsPerApiRaw.containsKey(apiIdentifier)) {
                  interactionsPerApiRaw.put(apiIdentifier, new HashMap<>());
                }

                Assertions.assertThatCode(
                        () -> {
                          byte[] interactionBytes = Files.readAllBytes(Paths.get(path.toString()));
                          JsonNode interactionJsonNode =
                              InstanceTestConfiguration.MAPPER.readValue(
                                  interactionBytes, JsonNode.class);
                          interactionsPerApiRaw.get(apiIdentifier).put(path, interactionJsonNode);
                        })
                    .doesNotThrowAnyException();

                return interactionsPerApiRaw;
              },
              (map, map2) -> {
                // This is not thread safe yet.
                // Needs refactor: https://stackoverflow.com/a/24316429/3947202
                throw new UnsupportedOperationException();
              });
    }
  }

  /** Returns a List of Scenario Path which adheres to a numeric pattern. */
  private List<Path> getEachScenarioPath(Path scenariosDirectory) throws IOException {
    try (Stream<Path> walk = Files.walk(scenariosDirectory, 1)) {
      return walk.sorted()
          .filter(Files::isDirectory)
          .filter(
              path -> {
                try {
                  Integer.parseInt(path.getFileName().toString());
                  return true;
                } catch (NumberFormatException e) {
                  return false;
                }
              })
          .collect(Collectors.toList());
    }
  }

  private List<Path> getSinkFixturesDirectoriesForScenario(Path kafkaFixturesPath)
      throws IOException {
    try (Stream<Path> walk = Files.walk(Paths.get(kafkaFixturesPath.toString()), 1)) {
      return walk.filter(Files::isDirectory)
          .filter(file -> !file.equals(kafkaFixturesPath))
          .collect(Collectors.toList());
    }
  }

  private List<Path> getFixtureFilePathsInDirectory(Path sinkFixtureDirectory) throws IOException {
    try (Stream<Path> walk = Files.walk(Paths.get(sinkFixtureDirectory.toString()), 1)) {
      return walk.sorted()
          .filter(file -> !Files.isDirectory(file))
          .filter(file -> file.toString().endsWith(".json"))
          .filter(
              file -> {
                try {
                  String fileName = file.getFileName().toString();
                  String withoutFileExtension =
                      fileName.substring(0, fileName.length() - ".json".length());
                  Integer.parseInt(withoutFileExtension);
                  return true;
                } catch (NumberFormatException e) {
                  return false;
                }
              })
          .collect(Collectors.toList());
    }
  }

  private void verifyRecordsForSinkTopic(
      Map.Entry<String, TestOutputTopic<JsonNode, JsonNode>> outputTopicEntry,
      KaHPPTestScenario scenario) {
    String outputTopicName = outputTopicEntry.getKey();

    assertNumberOfSinkRecords(
        outputTopicEntry.getValue().getQueueSize(), scenario, outputTopicName);

    List<TestRecord<JsonNode, JsonNode>> allProducedRecords =
        outputTopicEntry.getValue().readRecordsToList();

    for (TestRecord<JsonNode, JsonNode> producedRecord : allProducedRecords) {
      int index = allProducedRecords.indexOf(producedRecord);

      TestRecord<JsonNode, JsonNode> expectedRecord =
          scenario.getExpectedRecordsPerSink().get(outputTopicName).get(index);
      Path fixturePath = scenario.getPath().resolve(outputTopicName).resolve((index + 1) + ".json");

      assertRecord(fixturePath, producedRecord, expectedRecord);
    }
  }

  private void assertNumberOfSinkRecords(
      long actualNumberOfSinkRecords, KaHPPTestScenario scenario, String outputTopicName) {
    String description =
        "Number of records produced to sink topic '%s' does not match number of fixtures";

    Assertions.assertThat(actualNumberOfSinkRecords)
        .as(description, outputTopicName)
        .isEqualTo(
            scenario
                .getExpectedRecordsPerSink()
                .getOrDefault(outputTopicName, new ArrayList<>())
                .size());
  }

  protected static void assertRecord(
      Path path, TestRecord<JsonNode, JsonNode> actual, TestRecord<JsonNode, JsonNode> expected) {
    String fixture = path.subpath(path.getNameCount() - 4, path.getNameCount()).toString();
    String description = "Record '%s' does not match expected fixture: %s";

    SoftAssertions softly = new SoftAssertions();

    Assertions.assertThat(actual).as(description, "null", fixture).isNotNull();
    softly.assertThat(actual.getKey()).as(description, "key", fixture).isEqualTo(expected.getKey());
    softly
        .assertThat(actual.getValue())
        .as(description, "value", fixture)
        .isEqualTo(expected.getValue());
    softly
        .assertThat(actual.getRecordTime())
        .as(
            description + ", raw expected: %s, raw actual: %s",
            "recordTime",
            fixture,
            expected.getRecordTime().getEpochSecond(),
            actual.getRecordTime().getEpochSecond())
        .isEqualTo(expected.getRecordTime());

    // In order to match Headers we have to both:
    // - Add support on KaHPPTestRecord
    // - Ensure that the Header Helper as a frozen Clock, just like what we do
    // with the TopologyTestDriver
    //
    softly
        .assertThat(kafkaHeaderConverter.convert(actual.getHeaders()))
        .as(description, "headers", fixture)
        .isEqualTo(kafkaHeaderConverter.convert(expected.getHeaders()));

    softly.assertAll();
  }

  protected static KaHPPTestRecord readFixture(Path scenarioPath, String s) {
    AtomicReference<KaHPPTestRecord> testRecord = new AtomicReference<>();

    Assertions.assertThatCode(
            () -> {
              byte[] source = Files.readAllBytes(Paths.get(scenarioPath.toString(), s));
              JsonNode data = InstanceTestConfiguration.MAPPER.readTree(source);

              Assertions.assertThat(data.has("key")).isTrue();
              Assertions.assertThat(data.has("value")).isTrue();
              Assertions.assertThat(data.has("recordTime")).isTrue();

              JsonNode key = data.get("key");
              JsonNode value = data.get("value");
              JsonNode recordTime = data.get("recordTime");
              RecordHeaders headers = kafkaHeaderConverter.convert(data.get("headers"));

              testRecord.set(
                  new KaHPPTestRecord(
                      key.isNull() ? null : key,
                      value.isNull() ? null : value,
                      recordTime.isNull() ? null : Instant.ofEpochMilli(recordTime.asLong()),
                      headers));
            })
        .doesNotThrowAnyException();

    return testRecord.get();
  }
}
