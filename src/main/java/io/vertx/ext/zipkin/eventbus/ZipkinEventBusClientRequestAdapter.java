package io.vertx.ext.zipkin.eventbus;

import com.github.kristofa.brave.ClientRequestAdapter;
import com.github.kristofa.brave.IdConversion;
import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.http.BraveHttpHeaders;
import com.github.kristofa.brave.internal.Nullable;
import com.twitter.zipkin.gen.Endpoint;

import java.util.Collection;
import java.util.Collections;

import io.vertx.core.eventbus.Message;
import io.vertx.ext.zipkin.VertxKeys;

/**
 * TODO: document
 */
public class ZipkinEventBusClientRequestAdapter implements ClientRequestAdapter {
  private final Message msg;

  public ZipkinEventBusClientRequestAdapter(final Message msg) {

    this.msg = msg;
  }

  @Override
  public String getSpanName() {
    final String action = msg.headers().get(Headers.Action.getName());
    if (action != null) {
      return msg.address() + "::" + action;
    } else {
      return msg.address();
    }
  }

  // Vertx' messages resemble Http traffic so we're just going to encode our Zipkin tracing data
  // using similarly named headers.
  @Override
  public void addSpanIdToRequest(@Nullable SpanId spanId) {
    if (spanId == null) {
      msg.headers().add(BraveHttpHeaders.Sampled.getName(), "0");
    } else {
      msg.headers().add(BraveHttpHeaders.Sampled.getName(), "1");
      msg.headers().add(BraveHttpHeaders.TraceId.getName(), IdConversion.convertToString(spanId.traceId));
      msg.headers().add(BraveHttpHeaders.SpanId.getName(), IdConversion.convertToString(spanId.spanId));
      if (spanId.nullableParentId() != null) {
        msg.headers().add(BraveHttpHeaders.ParentSpanId.getName(), IdConversion.convertToString(spanId.parentId));
      }
    }
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    return Collections.emptyList();
  }

  @Override
  public Endpoint serverAddress() {
    return null;
  }
}
