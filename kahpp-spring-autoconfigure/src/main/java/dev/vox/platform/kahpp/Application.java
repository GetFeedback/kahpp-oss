package dev.vox.platform.kahpp;

import com.usabilla.healthcheck.springboot.kafka.KafkaStreamsHealthIndicator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(KafkaStreamsHealthIndicator.class)
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
