package com.mycompany.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.util.JUL;
import com.mycompany.web.filters.DetailLoggedFilter;
import com.mycompany.web.handlers.HomeHandler;
import com.mycompany.web.handlers.JdbcHandler;
import com.mycompany.web.handlers.RouterHandler;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import io.routekit.RouterSetup;

public class Server {
    static {
        JUL.initLogging();
    }
    private final static Logger logger = LoggerFactory.getLogger(Server.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ShutdownHooks shutdownHooks = new ShutdownHooks();
        ObjectMapper objectMapper = new ObjectMapper();
        var dbInit = new DatabaseInitializer(shutdownHooks);
        dbInit.createTables();
        dbInit.populateData();
        JdbcTemplate jdbc = dbInit.getJdbcTemplate();
        // var redisInit = new RedisInitializer(shutdownHooks);
        // StatefulRedisConnection<String, String> redis =
        // redisInit.getRedisConnection();
        TemplateEngine templateEngine = TemplateEngine.create(new ResourceCodeResolver("templates"),
                ContentType.Html);

        ThreadFactory factory = Thread.ofVirtual().name("vthread-", 0).factory();
        ExecutorService executor = Executors.newThreadPerTaskExecutor(factory);
        shutdownHooks.add(() -> executor.shutdown());
        HttpServer server = HttpServer.create(new InetSocketAddress(Config.get().serverPort), 0);
        server.setExecutor(executor);

        // Filter logBeforeFilter = Filter.beforeHandler("logBefore",
        // Server::loggingBeforeHandler);
        // Filter logAfterFilter = Filter.afterHandler("logAfter",
        // Server::loggingAfterHandler);
        List<HttpContext> httpContexts = new ArrayList<>();
        httpContexts.add(server.createContext("/", new RouterHandler(new RouterSetup<HttpHandler>()
                .add("/", new HomeHandler(templateEngine))
                .add("/jdbc", new JdbcHandler(jdbc, objectMapper))
                .build())));
        // httpContexts.add(server.createContext("/jdbc", Server::jdbcHandler));
        // httpContexts.add(server.createContext("/redis", Server::redisHandler));
        httpContexts.forEach(context -> {
            var filters = context.getFilters();
            // filters.add(logBeforeFilter);
            // filters.add(logAfterFilter);
            filters.add(new DetailLoggedFilter());
        });
        server.start();
        stopWatch.stop();
        logger.info("Server READY after {} seconds", stopWatch.getTotalTimeSeconds());

        shutdownHooks.add(() -> server.stop(1));
        shutdownHooks.activate();
    }

    static void rootHander(HttpExchange exchange) throws IOException {
    }

    static void jdbcHandler(HttpExchange exchange) throws IOException {
    }

    static void redisHandler(HttpExchange exchange) throws IOException {
        // var foo = redis.sync().get("foo");
        // if (foo == null) {
        // foo = "<null>";
        // }
        // var reply = foo.getBytes();
        // exchange.sendResponseHeaders(200, reply.length);
        // try (var os = exchange.getResponseBody()) {
        // os.write(reply);
        // }
    }

    static void loggingBeforeHandler(HttpExchange exchange) {
        logger.debug("{} {}", exchange.getRequestMethod(), exchange.getRequestURI());
    }

    static void loggingAfterHandler(HttpExchange exchange) {
        logger.debug("{} {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getResponseCode());
    }
}
