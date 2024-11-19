package com.mycompany.web.handlers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.mycompany.web.beans.IndexPage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import gg.jte.TemplateEngine;
import gg.jte.output.WriterOutput;

public class HomeHandler implements HttpHandler {

    private TemplateEngine templateEngine;

    public HomeHandler(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var page = new IndexPage("Java", "Hello 世界!");
        // try {
        // Thread.sleep(200);
        // } catch (InterruptedException e) {
        // }
        exchange.sendResponseHeaders(200, 0);
        try (var writer = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody()))) {
            templateEngine.render("index.jte", page, new WriterOutput(writer));
        }
    }

}
