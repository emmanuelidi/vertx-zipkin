package io.vertx.ext.zipkin.config;

import com.github.kristofa.brave.BoundarySampler;
import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.CountingSampler;
import com.github.kristofa.brave.EmptySpanCollector;
import com.github.kristofa.brave.LoggingSpanCollector;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.SpanCollector;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.zipkin.config.kafka.KafkaSpanCollectorBuilder;
import io.vertx.ext.zipkin.config.scribe.ScribeSpanCollectorBuilder;

/**
 * TODO: document
 */
public class BraveFactory {
  private static final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(BraveFactory.class);
  public static Brave create(final JsonObject json) {
    // TODO: not really doing any error checking on the input json.
    Brave.Builder builder = new Brave.Builder(json.getString("serviceName"));

    builder = applySampling(builder, json.getJsonObject("sampler", new JsonObject()));
    builder = applySpanCollector(builder, json.getJsonObject("collector", new JsonObject()));
    return builder.build();

  }

  private static Brave.Builder applySampling(final Brave.Builder builder, final JsonObject json) {
    Sampler sampler;
    float rate;
    switch (json.getString("type", "boundary")) {
      case "counting":
        rate = json.getFloat("rate", 0.01f);
        sampler = CountingSampler.create(rate);
        break;
      case "boundary":
      default:
        rate = json.getFloat("rate", 0.001f);
        sampler = BoundarySampler.create(rate);
        break;
    }
    logger.debug("Configuring sampler: {} at rate {}", sampler, rate);
    return builder.traceSampler(sampler);
  }

  private static Brave.Builder applySpanCollector(final Brave.Builder builder, final JsonObject json) {
    SpanCollector spanCollector;
    switch (json.getString("type")) {
      case "logging":
        spanCollector = new LoggingSpanCollector();
        break;
      case "scribe":
        spanCollector = ScribeSpanCollectorBuilder.build(json);
        break;
      case "kafka":
        spanCollector = KafkaSpanCollectorBuilder.build(json);
        break;
      case "empty":
      default:
        spanCollector = new EmptySpanCollector();
    }
    logger.debug("Configuring span collector: {}", spanCollector);
    return builder.spanCollector(spanCollector);
  }
}
