package com.mycompany.app;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.mycompany.util.JUL;
import com.sun.net.httpserver.HttpServer;

/**
 * Hello world!
 */
public class WebServer {

    static {
        JUL.initLogging();
    }

    public static void main(String[] args) throws IOException {
        var body = "hello world".getBytes();
        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        ThreadFactory factory = Thread.ofVirtual().name("executor-", 0).factory();
        server.setExecutor(Executors.newThreadPerTaskExecutor(factory));
        server.createContext("/").setHandler(exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        System.out.println("ready");
    }
}
