package com.mycompany.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import com.mycompany.web.beans.IndexPage;
import com.mycompany.web.beans.JdbcHandlerReq;
import com.mycompany.web.filters.DetailLoggedFilter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;
import gg.jte.resolve.ResourceCodeResolver;

public class Server {
    static {
        JUL.initLogging();
    }
    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private final static ObjectMapper objectMapper = new ObjectMapper();

    private static JdbcTemplate jdbc = null;
    // private static StatefulRedisConnection<String, String> redis = null;
    private static TemplateEngine templateEngine = TemplateEngine.create(new ResourceCodeResolver("templates"),
            ContentType.Html);

    public static void main(String[] args) throws IOException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        ShutdownHooks shutdownHooks = new ShutdownHooks();

        stopWatch.start();
        var dbInit = new DatabaseInitializer(shutdownHooks);
        dbInit.createTables();
        dbInit.populateData();
        jdbc = dbInit.getJdbcTemplate();

        // var redisInit = new RedisInitializer(shutdownHooks);
        // redis = redisInit.getRedisConnection();

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
        httpContexts.add(server.createContext("/", Server::rootHander));
        httpContexts.add(server.createContext("/jdbc", Server::jdbcHandler));
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
        var page = new IndexPage();
        page.title = "Java";
        page.content = "Hello 世界!";
        // try {
        // Thread.sleep(200);
        // } catch (InterruptedException e) {
        // }
        exchange.sendResponseHeaders(200, 0);
        try (var writer = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody()))) {
            templateEngine.render("index.jte", page, new WriterOutput(writer));
        }
    }

    static void jdbcHandler(HttpExchange exchange) throws IOException {
        JdbcHandlerReq req = objectMapper.readValue(exchange.getRequestBody(), JdbcHandlerReq.class);
        req.validate();

        var list = jdbc.queryForList("SELECT * FROM contacts WHERE email like ?", "%" + req.emailLike + "%");

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);
        try (var os = exchange.getResponseBody()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, list);
        }
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
