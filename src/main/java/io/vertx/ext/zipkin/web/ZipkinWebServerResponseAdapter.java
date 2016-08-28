package io.vertx.ext.zipkin.web;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerResponseAdapter;
import com.github.kristofa.brave.TraceKeys;

import java.util.ArrayList;
import java.util.Collection;

import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.zipkin.VertxKeys;

/**
 * TODO: document
 */
public class ZipkinWebServerResponseAdapter implements ServerResponseAdapter {
  private final RoutingContext context;

  public ZipkinWebServerResponseAdapter(final RoutingContext context) {
    this.context = context;
  }

  @Override
  public Collection<KeyValueAnnotation> responseAnnotations() {
    final Collection<KeyValueAnnotation> annotations = new ArrayList<>(4);

    final HttpServerResponse resp = context.response();
    annotations.add(KeyValueAnnotation.create(TraceKeys.HTTP_STATUS_CODE, String.valueOf(resp.getStatusCode())));

    if (context.failed()) {
      if (context.failure() instanceof ReplyException) {
        final ReplyException err = (ReplyException) context.failure();
        annotations.add(KeyValueAnnotation.create(VertxKeys.ReplyFailureType.getName(), err.failureType().name()));
        annotations.add(KeyValueAnnotation.create(VertxKeys.ReplyFailureCode.getName(), Integer.valueOf(err.failureCode()).toString()));
      }
      final String msg = context.failure().getMessage();
      annotations.add(KeyValueAnnotation.create(VertxKeys.ReplyFailureMessage.getName(), msg != null ? msg : ""));
    }
    return annotations;
  }
}
