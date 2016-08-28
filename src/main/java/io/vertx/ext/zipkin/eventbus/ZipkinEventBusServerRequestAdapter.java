package io.vertx.ext.zipkin.eventbus;

import com.github.kristofa.brave.IdConversion;
import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerRequestAdapter;
import com.github.kristofa.brave.SpanId;
import com.github.kristofa.brave.TraceData;
import com.github.kristofa.brave.http.BraveHttpHeaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import io.vertx.core.eventbus.Message;
import io.vertx.ext.zipkin.VertxKeys;

/**
 * TODO: document
 */
public class ZipkinEventBusServerRequestAdapter implements ServerRequestAdapter {
  private final Message msg;

  public ZipkinEventBusServerRequestAdapter(final Message msg) {
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

  @Override
  public TraceData getTraceData() {
    final String sampled = msg.headers().get(BraveHttpHeaders.Sampled.getName());
    if (sampled != null) {
      if ("0".equals(sampled) || "false".equals(sampled.toLowerCase())) {
        return TraceData.builder().sample(false).build();
      } else {
        final String parentSpanId = msg.headers().get(BraveHttpHeaders.ParentSpanId.getName());
        final String traceId = msg.headers().get(BraveHttpHeaders.TraceId.getName());
        final String spanId = msg.headers().get(BraveHttpHeaders.SpanId.getName());
        if (traceId != null && spanId != null) {
          final SpanId span = SpanId.builder()
              .traceId(IdConversion.convertToLong(traceId))
              .spanId(IdConversion.convertToLong(spanId))
              .parentId(parentSpanId == null ? null : IdConversion.convertToLong(parentSpanId))
              .build();
          return TraceData.builder().sample(true).spanId(span).build();
        }
      }
    }
    return TraceData.builder().build();
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    final String action = msg.headers().get(Headers.Action.getName());
    final Collection<KeyValueAnnotation> annotations = new ArrayList<>(2);

    annotations.add(KeyValueAnnotation.create(VertxKeys.EventbusService.getName(), msg.address()));
    if (action != null) {
      annotations.add(KeyValueAnnotation.create(VertxKeys.ServiceProxyAction.getName(), action));
    }
    return annotations;
  }
}
