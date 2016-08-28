package io.vertx.ext.zipkin.config.scribe;

import com.github.kristofa.brave.SpanCollector;
import com.github.kristofa.brave.scribe.ScribeSpanCollector;
import com.github.kristofa.brave.scribe.ScribeSpanCollectorParams;

import io.vertx.core.json.JsonObject;

/**
 * TODO: document
 */
public class ScribeSpanCollectorBuilder {
  public static SpanCollector build(final JsonObject json) {
    return new ScribeSpanCollector(json.getString("host", "localhost"),
        json.getInteger("port", 8410),
        getParams(json.getJsonObject("params", new JsonObject())));
  }

  private static ScribeSpanCollectorParams getParams(final JsonObject json) {
    final ScribeSpanCollectorParams params = new ScribeSpanCollectorParams();
    params.setBatchSize(json.getInteger("batchSize", params.getBatchSize()));
    params.setNrOfThreads(json.getInteger("threads", params.getNrOfThreads()));
    params.setQueueSize(json.getInteger("queueSize", params.getQueueSize()));
    params.setSocketTimeout(json.getInteger("socketTimeout", params.getSocketTimeout()));
    return params;
  }
}
