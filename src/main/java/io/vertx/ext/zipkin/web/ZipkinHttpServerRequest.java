package io.vertx.ext.zipkin.web;

import com.github.kristofa.brave.http.HttpServerRequest;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * TODO: document
 */
class ZipkinHttpServerRequest implements HttpServerRequest {
  private final io.vertx.core.http.HttpServerRequest request;

  protected ZipkinHttpServerRequest(final io.vertx.core.http.HttpServerRequest request) {
    this.request = request;
  }

  @Override
  public String getHttpHeaderValue(String headerName) {
      return request.getHeader(headerName);
  }

  @Override
  public URI getUri() {
      try {
        return new URI(request.absoluteURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
  }

  @Override
  public String getHttpMethod() {
      return request.method().name();
  }
}
