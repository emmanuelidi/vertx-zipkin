package io.vertx.ext.zipkin;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.ClientSpanThreadBinder;
import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.ServerSpanThreadBinder;
import com.twitter.zipkin.gen.Span;

import java.util.Map;
import java.util.function.BiFunction;

import io.vertx.core.Context;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.zipkin.logging.LoggingDC;

/**
 * TODO: document
 *
 * Propagate span bindings across context executions.
 */
public class ZipkinSchedulerInterceptor implements BiFunction<Context, Runnable, Runnable> {
  private final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ServerSpanThreadBinder serverSpanThreadBinder;
  private final ClientSpanThreadBinder clientSpanThreadBinder;

  public ZipkinSchedulerInterceptor(final ServerSpanThreadBinder serverSpanthreadBinder, final ClientSpanThreadBinder clientSpanThreadBinder) {
    this.serverSpanThreadBinder = serverSpanthreadBinder;
    this.clientSpanThreadBinder = clientSpanThreadBinder;
  }

  public ZipkinSchedulerInterceptor(final Brave brave) {
    this(brave.serverSpanThreadBinder(), brave.clientSpanThreadBinder());
  }

  @Override
  public Runnable apply(Context context, Runnable runnable) {
    final ServerSpan serverSpan = serverSpanThreadBinder.getCurrentServerSpan();
    final Span clientSpan = clientSpanThreadBinder.getCurrentClientSpan();
    final Map<String, String> dc = LoggingDC.getCopyOfContextMap();

    if (logger.isDebugEnabled() && (serverSpan.getSpan() != null || clientSpan != null)) {
      logger.debug("scheduled: client={}, server={}, context={}", clientSpan, serverSpan, context);
    }
    return () -> {
      LoggingDC.setContextMap(dc);
      if (logger.isDebugEnabled() && (serverSpan.getSpan() != null || clientSpan != null)) {
        logger.debug("executing: client={}, server={}, context={}", clientSpan, serverSpan, context);
      }
      serverSpanThreadBinder.setCurrentSpan(serverSpan);
      clientSpanThreadBinder.setCurrentSpan(clientSpan);
      try {
        runnable.run();
      } finally {
        serverSpanThreadBinder.setCurrentSpan(null);
        clientSpanThreadBinder.setCurrentSpan(null);
        LoggingDC.removeAll();
      }
    };
  }
}
