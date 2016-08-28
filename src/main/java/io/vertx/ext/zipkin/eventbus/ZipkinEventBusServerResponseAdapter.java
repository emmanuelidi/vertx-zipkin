package io.vertx.ext.zipkin.eventbus;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.ServerResponseAdapter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.zipkin.VertxKeys;

/**
 * TODO: document
 */
public class ZipkinEventBusServerResponseAdapter implements ServerResponseAdapter {
  private final Message msg;

  public ZipkinEventBusServerResponseAdapter(final Message msg) {
    this.msg = msg;
  }

  @Override
  public Collection<KeyValueAnnotation> responseAnnotations() {
    if (msg.body() instanceof ReplyException) {
      final ReplyException err = (ReplyException) msg.body();
      return Arrays.asList(
          KeyValueAnnotation.create(VertxKeys.ReplyFailureType.getName(), err.failureType().name()),
          KeyValueAnnotation.create(VertxKeys.ReplyFailureCode.getName(), Integer.valueOf(err.failureCode()).toString()),
          KeyValueAnnotation.create(VertxKeys.ReplyFailureMessage.getName(), err.getMessage() != null ? err.getMessage() : ""));
    } else {
      return Collections.emptyList();
    }
  }
}
