/**
 * TODO: document
 */
package io.vertx.ext.zipkin;

/*


18:14:00.427 [vert.x-eventloop-thread-3] INFO io.vertx.ext.zipkin.web.ZipkinHttpHandler - BEGIN GET http://localhost:8080/qotd
18:14:00.432 [vert.x-eventloop-thread-3] INFO io.vertx.ext.zipkin.examples.BootstrapVerticle - GET http://localhost:8080/qotd (start)
18:14:00.447 [vert.x-eventloop-thread-3] INFO io.vertx.ext.zipkin.examples.Main - Eventbus Message: address=services.fortune, replyAddress=1, headers=action: qotd

18:14:00.448 [vert.x-eventloop-thread-1] INFO io.vertx.ext.zipkin.examples.services.impl.FortuneServiceImpl - Received QOTD request
18:14:05.450 [vert.x-eventloop-thread-1] INFO io.vertx.ext.zipkin.examples.services.impl.FortuneServiceImpl - Sent QOTD response
18:14:05.451 [vert.x-eventloop-thread-1] INFO io.vertx.ext.zipkin.examples.Main - Eventbus Message: address=1, replyAddress=null, headers=
18:14:05.452 [vert.x-eventloop-thread-3] INFO io.vertx.ext.zipkin.examples.BootstrapVerticle - GET http://localhost:8080/qotd (completed)
18:14:05.461 [vert.x-eventloop-thread-3] INFO io.vertx.ext.zipkin.web.ZipkinHttpHandler - END GET http://localhost:8080/qotd (status:200, bytes:26)



 */