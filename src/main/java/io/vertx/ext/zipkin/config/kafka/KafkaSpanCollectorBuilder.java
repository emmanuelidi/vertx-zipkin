package io.vertx.ext.zipkin.config.kafka;

import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;
import com.github.kristofa.brave.SpanCollector;
import com.github.kristofa.brave.kafka.KafkaSpanCollector;

import io.vertx.core.json.JsonObject;

/**
 * TODO: document
 */
public class KafkaSpanCollectorBuilder {
  public static SpanCollector build(final JsonObject json) {
    KafkaSpanCollector.Config.Builder conf = KafkaSpanCollector.Config.builder(json.getString("bootstrapServers", "localhost"))
        .flushInterval(json.getInteger("flushInterval", 1))
        .topic(json.getString("topic", "zipkin"));

    return KafkaSpanCollector.create(conf.build(), new EmptySpanCollectorMetricsHandler());
  }

}
