package dev.vox.platform.kahpp.configuration;

/**
 * A RecordAction allows a KaHPP Step to choose what happens to a Record during its processing. <br>
 * Forward means it will be sent to the next Step (Kafka Streams Processor) <br>
 * See more about Record forwarding here: {@link
 * org.apache.kafka.streams.processor.ProcessorContext}
 */
public interface RecordAction {
  boolean shouldForward();
}
