package fr.afpa.backend.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Aucun accès implicite à localhost:5432, DB_URL ou au compte de développement. */
public abstract class IsolatedPostgres {
    public static String url() {
        String url = System.getenv("PONDERER_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/ponderer_test")
                || url.contains(":5432/")) {
            throw new IllegalStateException("Utilisez scripts/test-ponderer.sh : PostgreSQL jetable explicitement requis.");
        }
        return url;
    }
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url(), "ponderer_test", "ponderer_test");
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String isolatedUrl = url();
        registry.add("spring.datasource.url", () -> isolatedUrl);
        registry.add("spring.flyway.url", () -> isolatedUrl);
        registry.add("spring.flyway.user", () -> "ponderer_test");
        registry.add("spring.flyway.password", () -> "ponderer_test");
        registry.add("spring.datasource.username", () -> "ponderer_test");
        registry.add("spring.datasource.password", () -> "ponderer_test");
        registry.add("application.bootstrap.admin.email", () -> "");
        registry.add("application.bootstrap.admin.password", () -> "");
    }
}
