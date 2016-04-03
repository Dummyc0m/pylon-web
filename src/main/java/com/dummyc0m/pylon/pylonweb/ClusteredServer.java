package com.dummyc0m.pylon.pylonweb;

import com.dummyc0m.pylon.pyloncore.ConfigFile;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.File;

/**
 * Created by Dummyc0m on 3/23/16.
 */
public class ClusteredServer {
    public static void main(String[] args) {
        new ClusteredServer().launch(args);
    }

    private void launch(String[] args) {
        ConfigFile<ServerConfig> configFile = new ConfigFile<>(new File(System.getProperty("user.dir"), "config"),
                "config.json", ServerConfig.class);
        ServerConfig config = configFile.getConfig();
        ClusterManager mgr = new HazelcastClusterManager();

        VertxOptions options1 = new VertxOptions().setClusterManager(mgr);
        Vertx.clusteredVertx(options1, res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();
                setUpVertx(vertx, config);
            } else {
                //failed
                throw new RuntimeException("Failed to create a clustered vertx object");
            }
        });
    }

    private void setUpVertx(Vertx vertx, ServerConfig config) {
        EventBus bus = vertx.eventBus();
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        JsonObject params = new JsonObject()
                .put("url", config.getUrl())
                .put("user", config.getUser())
                .put("password", config.getPassward())
                .put("max_statements", 20)
                .put("max_statements_per_connection", 10)
                .put("max_idle_time", 300)
                .put("driver_class", "com.mysql.jdbc.Driver")
                .put("min_pool_size", 1);
        JDBCClient client = JDBCClient.createNonShared(vertx, params);

        client.getConnection(result -> {
            if(result.succeeded()) {
                SQLConnection connection = result.result();
                SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
                BridgeOptions options = new BridgeOptions()
                        .addInboundPermitted(new PermittedOptions()
                                .setAddress("pylon." + config.getInstanceName())
                                .setRequiredAuthority("admin.modify_controller"))
                        .addInboundPermitted(new PermittedOptions()
                                .setAddress("pylon." + config.getInstanceName())
                                .setRequiredAuthority("admin.modify_server"))
                        // Server specific permission here
                        .addInboundPermitted(new PermittedOptions()
                                .setAddress("pylon." + config.getInstanceName())
                                .setRequiredAuthority("user.access_server"))
                        .addInboundPermitted(new PermittedOptions()
                                .setAddress("pylon." + config.getInstanceName())
                                .setRequiredAuthority("moderator.access_controller"));
                sockJSHandler.bridge(options);
                router.route("/eventbus").handler(sockJSHandler);

                AuthProvider authProvider = JDBCAuth.create(client);
                AuthHandler authHandler = RedirectAuthHandler.create(authProvider);
                router.route().handler(CookieHandler.create());
                router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
                router.route("/login").handler(FormLoginHandler.create(authProvider));
                router.route("/").handler(authHandler);
                router.route().handler(StaticHandler.create());


                server.listen(config.getPort());
            } else {
                throw new RuntimeException("Failed to connect to db");
            }
        });
    }
}
