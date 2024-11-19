package com.mycompany.web.handlers;

import java.io.IOException;

import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.web.beans.JdbcHandlerReq;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class JdbcHandler implements HttpHandler {

    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;

    public JdbcHandler(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        JdbcHandlerReq req = objectMapper.readValue(exchange.getRequestBody(), JdbcHandlerReq.class);
        req.validate();

        var list = jdbc.queryForList("SELECT * FROM contacts WHERE email like ?", "%" + req.emailLike() + "%");

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, 0);
        try (var os = exchange.getResponseBody()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, list);
        }
    }

}
