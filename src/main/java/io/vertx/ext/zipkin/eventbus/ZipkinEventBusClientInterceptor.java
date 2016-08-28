package io.vertx.ext.zipkin.eventbus;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientRequestInterceptor;
import com.github.kristofa.brave.ClientResponseInterceptor;
import com.github.kristofa.brave.ClientSpanThreadBinder;
import com.github.kristofa.brave.LocalTracer;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerSpanThreadBinder;
import com.twitter.zipkin.gen.Span;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.Interceptor;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.SendContext;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.zipkin.logging.LoggingDC;

/**
 * Instruments Vertx on the send & publish side of eventbus RPC.
 *
 * Note: We're treating Verticle RPC calls as "client-server" communication. Since this processing
 * is occurring, potentially, within the same JVM (perhaps even on the same thread) we need to
 * re-establish both the client and server spans in TLS to avoid closing out the parent span too
 * early.
 */
public class ZipkinEventBusClientInterceptor implements Interceptor {
  private static final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(ZipkinEventBusClientInterceptor.class);

  // TODO: what happens if we don't get a response? How do we bound this `queue` and report errors?
  private final ConcurrentMap<String, SpanContext> pendingRequests = new ConcurrentHashMap<>(1000);

  private final ServerSpanThreadBinder serverSpanThreadBinder;
  private final ClientSpanThreadBinder spanThreadBinder;
  private final ClientRequestInterceptor requestInterceptor;
  private final ClientResponseInterceptor responseInterceptor;
  private final LocalTracer localTracer;

  public ZipkinEventBusClientInterceptor(final ServerSpanThreadBinder serverSpanThreadBinder,
                                         final ClientSpanThreadBinder spanThreadBinder,
                                         final ClientRequestInterceptor requestInterceptor,
                                         final ClientResponseInterceptor responseInterceptor,
                                         final LocalTracer localTracer) {

    this.serverSpanThreadBinder = serverSpanThreadBinder;
    this.spanThreadBinder = spanThreadBinder;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.localTracer = localTracer;
  }

  public ZipkinEventBusClientInterceptor(final Brave brave) {
    this(brave.serverSpanThreadBinder(), brave.clientSpanThreadBinder(),
        brave.clientRequestInterceptor(), brave.clientResponseInterceptor(), brave.localTracer());
  }

  @Override
  public void handle(SendContext context) {
    final Message msg = context.message();

    if (!context.send()) {
      // TODO: How should we handle publish() events? Currently, we'll see the traffic on the receive side only
      //onPublish(msg);
    } else if (msg.replyAddress() != null) {
      // All client initiated point-to-point traffic should contain a reply address. Else, we're
      // looking at a send from the server response.
      clientSend(msg);
    }
    context.next();
  }

  @Override
  public void handleDelivery(DeliveryContext context) {
    final Message msg = context.message();
    if (context.send()) {
      final SpanContext span = pendingRequests.remove(msg.address());
      if (span != null) {
        // This is a response that we've been waiting for so close it out
        clientReceive(span.serverSpan, span.clientSpan, msg);
      }
    }
    context.next();
  }

  // Create a Local Component span. We won't know response time but we can see the "chattiness" of
  // the components.
//  private void onPublish(final Message msg) {
//    if (logger.isDebugEnabled()) {
//      logger.debug("Publish.Send: address={}, reply={}, headers={}", msg.address(), msg.replyAddress(), msg.headers());
//    }
//
//    localTracer.startNewSpan(msg.address(), "publish");
//    localTracer.finishSpan();
//  }

  /**
   * Start a Client Send span
   *
   * Note: we save both the current client and overarching server spans in order to re-apply on the
   * response.
   */
  private void clientSend(final Message msg) {
    requestInterceptor.handle(new ZipkinEventBusClientRequestAdapter(msg));
    final ServerSpan serverSpan = serverSpanThreadBinder.getCurrentServerSpan();
    final Span span = spanThreadBinder.getCurrentClientSpan();

    if (logger.isDebugEnabled()) {
      logger.debug("Client.Send ({}): address={}, reply={}, headers={}", span, msg.address(), msg.replyAddress(), msg.headers());
    }

    // We're exploiting the eventbus impl which generates reply addresses into the local jvm.
    // This would be a problem if clients were able to dictate specific reply addresses. :(
    pendingRequests.put(msg.replyAddress(), new SpanContext(serverSpan, span));
  }

  // Start a Client Receive.
  private void clientReceive(final ServerSpan serverSpan, final Span span, final Message msg) {
    serverSpanThreadBinder.setCurrentSpan(serverSpan);
    spanThreadBinder.setCurrentSpan(span);
    responseInterceptor.handle(new ZipkinEventBusClientResponseAdapter(msg));

    // Re-establish the logging context once we continue with processing the response.
    LoggingDC.put(span);

    if (logger.isDebugEnabled()) {
      logger.debug("Client.Receive ({}): address={}, reply={}, headers={}", span, msg.address(), msg.replyAddress(), msg.headers());
    }
  }

  private static class SpanContext {
    final ServerSpan serverSpan;
    final Span clientSpan;

    SpanContext(final ServerSpan serverSpan, final Span clientSpan) {
      this.serverSpan = serverSpan;
      this.clientSpan = clientSpan;
    }
  }
}
