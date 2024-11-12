package com.mycompany.web.filters;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class DetailLoggedFilter extends Filter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        var requestHeaders = exchange.getRequestHeaders();
        var responseHeaders = exchange.getResponseHeaders();
        var newRequestBody = new BufferRecordedInputStream(requestHeaders, exchange.getRequestBody());
        var newResponseBody = new BufferRecordedOutputStream(responseHeaders, exchange.getResponseBody());
        var newExchange = new DelegatingHttpExchange(exchange) {
            public InputStream getRequestBody() {
                return newRequestBody;
            }

            public OutputStream getResponseBody() {
                return newResponseBody;
            };
        };
        try {
            chain.doFilter(newExchange);
        } finally {
            logger.debug("REQUEST headers - {}", stringifyHeaders(requestHeaders));
            logger.debug("REQUEST body - {}", newRequestBody);
            logger.debug("RESPONSE headers - {}", stringifyHeaders(responseHeaders));
            logger.debug("RESPONSE body - {}", newResponseBody);
            stopWatch.stop();
            logger.info("{} {} {} - {}ms", exchange.getRequestMethod(), exchange.getRequestURI(),
                    exchange.getResponseCode(), stopWatch.getTotalTime(TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public String description() {
        return "a filter that logs HTTP response body";
    }

    static String stringifyHeaders(Headers headers) {
        var sb = new StringBuilder();
        sb.append("{ ");
        headers.forEach((key, valList) -> {
            sb.append(key);
            sb.append('=');
            sb.append(valList);
            sb.append(' ');
        });
        sb.append("}");
        return sb.toString();
    }

}
