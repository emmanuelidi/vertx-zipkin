package io.vertx.ext.zipkin.web;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ServerRequestInterceptor;
import com.github.kristofa.brave.ServerResponseInterceptor;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerSpanThreadBinder;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.SpanNameProvider;

import java.util.Objects;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.zipkin.ShareableBrave;
import io.vertx.ext.zipkin.logging.LoggingDC;


/**
 * TODO: document
 */
public final class ZipkinWebHandler implements Handler<RoutingContext> {
  private static final String HTTP_HEADER_TRACE_ID = "X-Trace-ID";

  private final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(this.getClass());


  private final ServerSpanThreadBinder spanThreadBinder;
  private final ServerRequestInterceptor requestInterceptor;
  private final ServerResponseInterceptor responseInterceptor;
  private final SpanNameProvider spanNameProvider;

  public ZipkinWebHandler(final ServerSpanThreadBinder spanThreadBinder,
                          final ServerRequestInterceptor requestInterceptor,
                          final ServerResponseInterceptor responseInterceptor,
                          final SpanNameProvider spanNameProvider) {
    this.spanThreadBinder = spanThreadBinder;
    this.requestInterceptor = requestInterceptor;
    this.responseInterceptor = responseInterceptor;
    this.spanNameProvider = spanNameProvider;
  }

  public ZipkinWebHandler(final ServerSpanThreadBinder spanThreadBinder,
                          final ServerRequestInterceptor requestInterceptor,
                          final ServerResponseInterceptor responseInterceptor) {
    this(spanThreadBinder, requestInterceptor, responseInterceptor, new DefaultSpanNameProvider());
  }

  public ZipkinWebHandler(final Brave brave) {
    this(brave.serverSpanThreadBinder(), brave.serverRequestInterceptor(), brave.serverResponseInterceptor());
  }

  /**
   * Convenience factory method to create from shared Brave configuration in the Vertx localMap.
   */
  public static ZipkinWebHandler create(final Vertx vertx) {
    final LocalMap<String, ShareableBrave> localMap = vertx.sharedData().<String, ShareableBrave>getLocalMap(ShareableBrave.LOCALMAP_KEY);
    Objects.requireNonNull(localMap, "localmap==null");
    final ShareableBrave brave = localMap.get(ShareableBrave.LOCALMAP_ITEM_KEY);
    Objects.requireNonNull(brave, "brave==null");
    return new ZipkinWebHandler(brave.get());
  }

  @Override
  public void handle(RoutingContext context) {
    final HttpServerRequest req = context.request();

    requestInterceptor.handle(new ZipkinWebServerRequestAdapter(new ZipkinHttpServerRequest(req), spanNameProvider));
    final ServerSpan span = spanThreadBinder.getCurrentServerSpan();

    LoggingDC.put(span);

    if (logger.isDebugEnabled()) {
      logger.debug("Request.Started ({}): {} {} [{}]", span, req.method(), req.absoluteURI());
    }
    context.addHeadersEndHandler(v -> {
      if (span.getSample()) {
        context.response().putHeader(HTTP_HEADER_TRACE_ID, LoggingDC.toHexString(span.getSpan().getTrace_id()));
      }
    });

    context.addBodyEndHandler(v -> {
      if (logger.isDebugEnabled()) {
        logger.debug("Request.Completed ({}): {} {}", span, req.method(), req.absoluteURI());
      }
      spanThreadBinder.setCurrentSpan(span);
      responseInterceptor.handle(new ZipkinWebServerResponseAdapter(context));
      LoggingDC.removeAll();
    });
    context.next();
  }
}
