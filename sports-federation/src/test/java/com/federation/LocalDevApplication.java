package com.federation;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Runs the backend against an embedded PostgreSQL 16 — no Docker or local
 * database installation needed:
 *
 * <pre>
 *   mvn spring-boot:test-run
 * </pre>
 *
 * Data is kept in {@code target/dev-pgdata} between runs ({@code mvn clean} resets it);
 * pass {@code -Ddev.db.dir=} for a throw-away database.
 * The database listens on port 5433 so it never clashes with a local PostgreSQL.
 */
public class LocalDevApplication {

    public static void main(String[] args) throws IOException {
        EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder()
                .setPort(Integer.parseInt(System.getProperty("dev.db.port", "5433")));
        // -Ddev.db.dir= (empty) uses a throw-away temp directory instead of target/dev-pgdata
        String dataDir = System.getProperty("dev.db.dir", "target/dev-pgdata");
        if (!dataDir.isBlank()) {
            builder.setDataDirectory(Path.of(dataDir).toAbsolutePath()).setCleanDataDirectory(false);
        }
        EmbeddedPostgres postgres = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { postgres.close(); } catch (IOException ignored) { }
        }));

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");

        SpringApplication.run(SportsFederationApplication.class, args);
    }
}
