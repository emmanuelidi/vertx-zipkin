package io.vertx.ext.zipkin;

/**
 * TODO: document
 */
public enum VertxKeys {

  EventbusService("vertx.service"),
  ServiceProxyAction("vertx.service.action"),
  ReplyFailureType("vertx.reply_failure_type"),
  ReplyFailureCode("vertx.reply_failure_code"),
  ReplyFailureMessage("vertx.reply_failure_message");

  private final String name;

  VertxKeys(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
