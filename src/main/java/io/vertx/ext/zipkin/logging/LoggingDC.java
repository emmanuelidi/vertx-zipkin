package io.vertx.ext.zipkin.logging;

import com.github.kristofa.brave.ServerSpan;
import com.github.kristofa.brave.SpanId;
import com.twitter.zipkin.gen.Span;

import org.slf4j.MDC;

import java.util.Map;

/**
 * TODO: document
 *
 * Vertx has, infuriatingly, decided to provide yet another wrapper around logging facilities and
 * ignored one of the most important logging features for a server environment: the MDC.
 *
 * For now, while the approach is being testing, we're just ignoring the fact that SLF4j isn't
 * necessarily going to be available and assume that it will be to make life easier. All calls are
 * delegated to the SLF4j driver.
 */
public final class LoggingDC {
  public static final String TRACE_ID = "zipkin.trace_id";


  public static Map<String, String> getCopyOfContextMap() {
    return MDC.getCopyOfContextMap();
  }

  public static void setContextMap(Map<String, String> contextMap) {
    if (contextMap != null) {
      MDC.setContextMap(contextMap);
    }
  }

  public static void put(String key, String val) throws IllegalArgumentException {
    MDC.put(key, val);
  }

  public static String get(String key) throws IllegalArgumentException {
    return MDC.get(key);
  }

  public static void remove(String key) throws IllegalArgumentException {
    MDC.remove(key);
  }

  public static void clear() {
    MDC.clear();
  }

  /**
   * Remove all Zipkin specific logging context variables.
   */
  public static void removeAll() {
    MDC.remove(TRACE_ID);
  }

  public static void put(final ServerSpan serverSpan) {
    if (serverSpan.getSpan() != null) {
      put(serverSpan.getSpan());
    } else {
      removeAll();
    }
  }

  public static void put(final Span span) {
    if (span != null) {
      put(TRACE_ID, toHexString(span.getTrace_id()));
    } else {
      removeAll();
    }
  }

  public static void put(final SpanId id) {
    if (id != null) {
      put(TRACE_ID, toHexString(id.traceId));
    } else {
      removeAll();
    }
  }

  // TODO(ei): need a better long-to-hex-string
  public static String toHexString(final long n) {
    return Long.toHexString(n);
  }
}
