package com.mycompany.web.handlers;

import java.io.IOException;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.routekit.Match;
import io.routekit.Router;

public class RouterHandler implements HttpHandler {

    Router<HttpHandler> router;

    public RouterHandler(Router<HttpHandler> router) {
        this.router = router;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Match<HttpHandler> match = router.routeOrNull(exchange.getRequestURI().toString());
        if (match == null)
            throw new NoSuchElementException("404 Not Found");
        match.handler().handle(exchange);
    }

}
