package io.vertx.ext.zipkin.eventbus;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.LocalTracer;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerSpanThreadBinder;
import com.github.kristofa.brave.SpanId;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.Interceptor;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.SendContext;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.zipkin.logging.LoggingDC;

/**
 * TODO: document
 */
public class ZipkinEventBusServerInterceptor implements Interceptor {
  private static final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(ZipkinEventBusServerInterceptor.class);

  // TODO: what happens if we don't get a response? How do we bound this `queue` and report errors?
  private final ConcurrentMap<String, ServerSpan> pendingResponses = new ConcurrentHashMap<>(1000);

  private final ServerSpanThreadBinder spanThreadBinder;
  private final ServerRequestInterceptor requestInterceptor;
  private final ServerResponseInterceptor responseInterceptor;
  private final LocalTracer localTracer;

  public ZipkinEventBusServerInterceptor(final ServerSpanThreadBinder spanThreadBinder,
                                         final ServerRequestInterceptor requestInterceptor,
                                         final ServerResponseInterceptor responseInterceptor,
                                         final LocalTracer localTracer) {
    this.spanThreadBinder = spanThreadBinder;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.localTracer = localTracer;
  }

  public ZipkinEventBusServerInterceptor(final Brave brave) {
    this(brave.serverSpanThreadBinder(), brave.serverRequestInterceptor(), brave.serverResponseInterceptor(), brave.localTracer());
  }

  @Override
  public void handleDelivery(DeliveryContext context) {
    final Message msg = context.message();

    if (!context.send()) {
      // A public message was delivered. Process the request in the context of a span
      onCalled(context, msg);
    } else {
      if (msg.replyAddress() != null) {
        serverReceive(msg);
      }
      context.next();
    }
  }

  @Override
  public void handle(SendContext context) {
    final Message msg = context.message();

    // We see all send()'s so ignore those we know are not server responses
    if (context.send() && msg.replyAddress() == null) {
      // If we have a span for the expected address (formerly, the replyAddress) then we can close out the span.
      final ServerSpan span = pendingResponses.remove(msg.address());
      if (span != null) {
        serverRespond(span, msg);
      }
    }
    context.next();
  }

  private void serverReceive(final Message msg) {
    requestInterceptor.handle(new ZipkinEventBusServerRequestAdapter(msg));
    final ServerSpan span = spanThreadBinder.getCurrentServerSpan();

    LoggingDC.put(span);

    if (logger.isDebugEnabled()) {
      logger.debug("Server.Receive ({}): address={}, reply={}, headers={}", span, msg.address(), msg.replyAddress(), msg.headers());
    }

    pendingResponses.put(msg.replyAddress(), span);
  }

  private void serverRespond(final ServerSpan span, final Message msg) {
    if (logger.isDebugEnabled()) {
      logger.debug("Server.Respond ({}): address={}, reply={}, headers={}", span, msg.address(), msg.replyAddress(), msg.headers());
    }

    spanThreadBinder.setCurrentSpan(span);
    responseInterceptor.handle(new ZipkinEventBusServerResponseAdapter(msg));

    LoggingDC.removeAll();
  }

  // TODO: need to test that this is actually useful
  // NOTE: currently, these should be connected to the calling trace due to the scheduler
  // interceptor. But, since the caller will return immediately, this span will exceed the duration
  // of the calling span...might look funky.
  private void onCalled(final DeliveryContext context, final Message msg) {
    final String action = msg.headers().get(Headers.Action.getName());
    final SpanId span = localTracer.startNewSpan(msg.address(), msg.address());

    LoggingDC.put(span);

    if (logger.isDebugEnabled()) {
      logger.debug("Publish.Receive ({}): address={}, reply={}, headers={}", action, msg.address(), msg.replyAddress(), msg.headers());
    }

    try {
      context.next();
    } finally {
      localTracer.finishSpan();

      LoggingDC.removeAll();
    }
  }
}
