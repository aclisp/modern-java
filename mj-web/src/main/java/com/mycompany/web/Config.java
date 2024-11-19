package com.mycompany.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public record Config(
        int serverPort,
        String redisURI,
        String h2URI,
        int h2Port) {

    private final static Logger logger = LoggerFactory.getLogger(Config.class);
    private static Config config = null;

    public Config() {
        this(8080,
                "redis://127.0.0.1:6379",
                "./database",
                28080);
    }

    public static Config get() {
        if (config != null)
            return config;

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Save default values
            Path path = Path.of("config.json").toAbsolutePath();
            String file = System.getProperty("config.file");
            if (file == null && !Files.exists(path))
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(Files.newOutputStream(path), new Config());

            // Load from config file
            if (file == null)
                file = path.toString();
            logger.info("config.file=" + file);
            config = objectMapper.readValue(new File(file), Config.class);
            return config;
        } catch (IOException ex) {
            throw new RuntimeException("Config ERROR", ex);
        }
    }
}
