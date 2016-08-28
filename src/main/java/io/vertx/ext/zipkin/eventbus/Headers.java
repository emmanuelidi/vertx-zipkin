package io.vertx.ext.zipkin.eventbus;

/**
 * TODO: document
 */
public enum Headers {
  Action("action");

  private final String name;

  Headers(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

}
