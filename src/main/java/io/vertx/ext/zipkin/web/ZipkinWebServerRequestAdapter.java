package io.vertx.ext.zipkin.web;

import com.github.kristofa.brave.KeyValueAnnotation;
import com.github.kristofa.brave.TraceKeys;
import com.github.kristofa.brave.http.HttpServerRequest;
import com.github.kristofa.brave.http.HttpServerRequestAdapter;
import com.github.kristofa.brave.http.SpanNameProvider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO: document
 */
public class ZipkinWebServerRequestAdapter extends HttpServerRequestAdapter {
  private final HttpServerRequest serverRequest;

  public ZipkinWebServerRequestAdapter(final HttpServerRequest serverRequest, final SpanNameProvider spanNameProvider) {
    super(serverRequest, spanNameProvider);
    this.serverRequest = serverRequest;
  }

  @Override
  public Collection<KeyValueAnnotation> requestAnnotations() {
    final Collection<KeyValueAnnotation> annotations = new ArrayList<>(3);
    annotations.add(KeyValueAnnotation.create(TraceKeys.HTTP_METHOD, serverRequest.getHttpMethod()));
    annotations.add(KeyValueAnnotation.create(TraceKeys.HTTP_PATH, serverRequest.getUri().getPath()));
    annotations.add(KeyValueAnnotation.create(TraceKeys.HTTP_HOST, serverRequest.getUri().getHost()));
    annotations.addAll(super.requestAnnotations());
    return annotations;
  }
}
