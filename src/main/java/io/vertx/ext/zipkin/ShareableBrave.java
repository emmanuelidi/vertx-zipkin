package io.vertx.ext.zipkin;

import com.github.kristofa.brave.Brave;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.Shareable;

/**
 * TODO: document
 * Used to pass around the configured Brave within the Vertx local SharedMap.
 */
public final class ShareableBrave implements Shareable {
  public static final String LOCALMAP_KEY = "zipkin";
  public static final String LOCALMAP_ITEM_KEY = "brave";

  private final Brave brave;

  public ShareableBrave(final Brave brave) {
    this.brave = brave;
  }
  public Brave get() {
    return this.brave;
  }

  public static boolean isShared(final Vertx vertx) {
    return vertx.sharedData().getLocalMap(LOCALMAP_KEY).get(LOCALMAP_ITEM_KEY) != null;
  }

  public static void share(final Vertx vertx, final Brave brave) {
    vertx.sharedData().getLocalMap(LOCALMAP_KEY).put(LOCALMAP_ITEM_KEY, new ShareableBrave(brave));
  }
}
