package com.mycompany.web;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseInitializer {

    private JdbcTemplate jdbc;

    public DatabaseInitializer(ShutdownHooks shutdownHooks) {
        var config = new HikariConfig();
        List<String> settings = List.of(
                "AUTO_SERVER=TRUE",
                "AUTO_SERVER_PORT=" + Config.get().h2Port(),
                "MODE=PostgreSQL",
                "DATABASE_TO_LOWER=TRUE",
                "DEFAULT_NULL_ORDERING=HIGH");
        config.setJdbcUrl("jdbc:h2:" + Config.get().h2URI() + ";" + String.join(";", settings));
        var ds = new HikariDataSource(config);
        shutdownHooks.add(() -> ds.close());
        var jdbc = new JdbcTemplate(ds);

        this.jdbc = jdbc;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbc;
    }

    public void createTables() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS contacts (
                    contact_id INTEGER PRIMARY KEY,
                    first_name VARCHAR(50) NOT NULL,
                    last_name VARCHAR(50) NOT NULL,
                    email VARCHAR(50) NOT NULL UNIQUE,
                    phone VARCHAR(50) NOT NULL UNIQUE
                );
                """);
    }

    public void populateData() {
        var data = """
                INSERT INTO contacts (contact_id, first_name, last_name,
                    email, phone) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """;
        jdbc.update(data, 1, "Hello", "世界", "hello@world.com", "");
        jdbc.update(data, 2, "Hello2", "世界", "hello2@world.com", "1002");
        jdbc.update(data, 3, "Hello3", "世界", "3hello@world.com", "1003");
    }
}
