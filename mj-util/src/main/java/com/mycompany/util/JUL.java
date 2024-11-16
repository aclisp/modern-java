package com.mycompany.util;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class JUL {
    // must set before the Logger
    public static void initLogging() {
        String name = "logging.properties";
        URL url = JUL.class.getClassLoader().getResource(name);
        String path = url.getFile();
        if (path.endsWith(".jar!/" + name)) {
            Path cur = Path.of(name).toAbsolutePath();
            if (!Files.exists(cur)) {
                try (var is = url.openStream()) {
                    Files.copy(is, cur);
                } catch (IOException ex) {
                    System.err.println(ex.toString());
                }
            }
            path = cur.toString();
        }
        System.out.println("java.util.logging.config.file=" + path);
        System.setProperty("java.util.logging.config.file", path);
    }
}
