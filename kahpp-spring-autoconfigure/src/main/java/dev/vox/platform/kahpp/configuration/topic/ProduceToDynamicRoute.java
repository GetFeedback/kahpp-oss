package dev.vox.platform.kahpp.configuration.topic;

public interface ProduceToDynamicRoute extends Produce {
  String STEP_CONFIGURATION_ERROR_TOPIC = "errorTopic";
  String STEP_CONFIGURATION_ROUTES = "routes";
  String STEP_CONFIGURATION_ROUTE_JMES_PATH = "jmesPath";
  String STEP_CONFIGURATION_ROUTE_TOPIC = "topic";
}
