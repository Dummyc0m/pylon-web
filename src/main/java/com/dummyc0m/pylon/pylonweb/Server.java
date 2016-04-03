package com.dummyc0m.pylon.pylonweb;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Created by Dummyc0m on 3/10/16.
 */
public class Server extends AbstractVerticle {
    @Override
    public void start(Future<Void> fut) {
        vertx.createHttpServer()
                .requestHandler(r -> {
                    r.response().putHeader("content-type", "text/html")
                            .end("<h1>Hello from my first " +
                                    "Vert.x 3 application</h1>");
                })
                .listen(config().getInteger("http.port", 8080), result -> {
                    if (result.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(result.cause());
                    }
                });
    }
}
