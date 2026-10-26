package fr.afpa.backend.migration;

import fr.afpa.backend.support.IsolatedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class PondererMigrationTest {
    private String schema;
    private Connection db;
    private static final List<String> TABLES = List.of("personne", "eleve", "enseignant", "responsable",
            "compte_utilisateur", "classe", "matiere", "periode", "inscription", "scolarite",
            "enseignement", "evaluation", "note", "bulletin", "ligne_bulletin", "responsabilite_legale");

    @BeforeEach void prepare() throws Exception {
        schema = "ponderer_" + UUID.randomUUID().toString().replace("-", "");
        flyway("1").migrate();
        db = connection();
    }
    @AfterEach void cleanup() throws Exception {
        if (db != null) db.close();
        if (schema != null) try (Connection c = IsolatedPostgres.connect(); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }
    private Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(IsolatedPostgres.url(), "ponderer_test", "ponderer_test")
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }
    private Connection connection() throws Exception {
        Connection c = IsolatedPostgres.connect();
        c.setSchema(schema);
        try (Statement st = c.createStatement()) {
            st.execute("SET statement_timeout = '10s'");
            st.execute("SET lock_timeout = '5s'");
        }
        return c;
    }
    private void sql(String sql) throws Exception { try (Statement st = db.createStatement()) { st.execute(sql); } }
    private String scalar(String sql) throws Exception {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getString(1); }
    }
    private Map<String, String> snapshot() throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        for (String table : TABLES) result.put(table, scalar("SELECT coalesce(jsonb_agg(row ORDER BY row::text)::text,'[]') FROM (SELECT to_jsonb(t) AS row FROM " + table + " t) x"));
        return result;
    }
    private void seed() throws Exception {
        sql("""
            INSERT INTO personne(id_personne,nom,prenom) VALUES (1,'Test','Prof'),(2,'Test','Eleve');
            INSERT INTO enseignant VALUES (1,'PROF-TEST');
            INSERT INTO eleve(id_personne,matricule,date_naissance) VALUES (2,'TEST-E','2014-01-02');
            INSERT INTO classe(id_classe,nom,niveau,annee_scolaire) VALUES
                (25,'A','6','2026-2027'),(30,'B','6','2026-2027'),(42,'C','6','2026-2027'),(99,'Vide','6','2026-2027');
            INSERT INTO matiere(id_matiere,code,nom) VALUES (28,'M','Maths'),(32,'F','Français'),(41,'H','Histoire');
            INSERT INTO enseignement(id_enseignement,id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut,actif) VALUES
                (1,1,25,28,2,'2026-09-01',true),(2,1,30,32,2,'2026-09-01',true),(3,1,42,41,2,'2026-09-01',true),
                (4,1,25,28,2,'2025-09-01',false);
            INSERT INTO periode(id_periode,libelle,date_debut,date_fin,statut) VALUES (1,'T1','2026-09-01','2026-12-31','OUVERTE');
            INSERT INTO scolarite(id_scolarite,id_eleve,id_classe,date_debut,statut) VALUES (1,2,25,'2026-09-01','ACTIVE');
            INSERT INTO inscription(id_inscription,id_eleve,date_inscription,statut) VALUES (1,2,'2026-09-01','ACTIVE');
            INSERT INTO evaluation(id_evaluation,id_enseignement,id_periode,libelle,date_evaluation,type_evaluation,coefficient_evaluation,bareme)
                VALUES (1,1,1,'Test','2026-10-01','DEVOIR',3,25);
            INSERT INTO note(id_note,id_scolarite,id_evaluation,valeur,commentaire,statut_note,date_saisie)
                VALUES (1,1,1,18,'Conserver','VALIDEE','2026-10-01T12:00:00Z');
            INSERT INTO bulletin(id_bulletin,id_scolarite,id_periode,id_validateur,date_generation,moyenne_generale,appreciation_generale,statut)
                VALUES (1,1,1,1,'2026-10-01',14.40,'Historique','BROUILLON');
            INSERT INTO ligne_bulletin(id_ligne_bulletin,id_bulletin,id_enseignement,texte,statut)
                VALUES (1,1,1,'Texte historique','BROUILLON');
            """);
    }
    private void assertConsistent() throws Exception {
        assertThat(scalar("""
            SELECT count(*) FROM enseignement e LEFT JOIN ponderer p USING(id_classe,id_matiere)
            WHERE p.id_classe IS NULL OR e.coefficient_matiere IS DISTINCT FROM p.coefficient_matiere
            """)).isEqualTo("0");
    }
    @Test void migrateEmptyV1AndValidateChecksum() throws Exception {
        var result = flyway(null).migrate();
        assertThat(result.migrationsExecuted).isEqualTo(1);
        flyway(null).validate();
        assertThat(scalar("SELECT count(*) FROM ponderer")).isEqualTo("0");
        try (var input = getClass().getResourceAsStream("/db/migration/V1__create_initial_schema.sql")) {
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())))
                    .isEqualTo("f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee");
        }
    }
    @Test void backfillAllIncludingInactiveAndPreserveEveryOriginalValue() throws Exception {
        seed(); var before = snapshot();
        flyway(null).migrate();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(scalar("SELECT jsonb_agg(to_jsonb(p) ORDER BY id_classe)::text FROM ponderer p"))
                .contains("25", "28", "30", "32", "42", "41", "2.00");
        assertThat(scalar("SELECT count(*) FROM ponderer")).isEqualTo("3");
        assertThat(scalar("SELECT count(*) FROM ponderer WHERE id_classe=99")).isEqualTo("0");
        assertConsistent();
    }
    @Test void conflictingHistoricalCoefficientAbortsAtomically() throws Exception {
        seed(); sql("UPDATE enseignement SET coefficient_matiere=3 WHERE id_enseignement=4");
        assertFailedMigrationPreservesV1();
    }
    @ParameterizedTest @ValueSource(strings = {"0", "-1", "'NaN'", "NULL"})
    void invalidHistoricalCoefficientAbortsAtomically(String coefficient) throws Exception {
        seed();
        sql("ALTER TABLE enseignement DROP CONSTRAINT ck_enseignement_coefficient");
        sql("ALTER TABLE enseignement ALTER COLUMN coefficient_matiere DROP NOT NULL");
        sql("UPDATE enseignement SET coefficient_matiere=" + coefficient + " WHERE id_enseignement=4");
        assertFailedMigrationPreservesV1();
    }
    private void assertFailedMigrationPreservesV1() throws Exception {
        var before = snapshot();
        assertThatThrownBy(() -> flyway(null).migrate()).hasStackTraceContaining("coefficients incohérents");
        assertThat(snapshot()).isEqualTo(before);
        assertThat(scalar("SELECT to_regclass('" + schema + ".ponderer') IS NULL")).isEqualTo("t");
        assertThat(scalar("SELECT count(*) FROM flyway_schema_history WHERE version='2'")).isEqualTo("0");
    }
    @Test void sqlConstraintsEnforcePositivityUniquenessAndReferences() throws Exception {
        seed(); flyway(null).migrate();
        for (String coefficient : List.of("0", "-1", "'NaN'", "NULL")) {
            assertThatThrownBy(() -> sql("INSERT INTO ponderer VALUES (99,28," + coefficient + ")")).isInstanceOf(SQLException.class);
        }
        assertThatThrownBy(() -> sql("INSERT INTO ponderer VALUES (25,28,2)")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> sql("INSERT INTO ponderer VALUES (9999,28,2)")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> sql("INSERT INTO ponderer VALUES (99,9999,2)")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> sql("DELETE FROM ponderer WHERE id_classe=25")).isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> sql("UPDATE ponderer SET id_classe=99 WHERE id_classe=25")).hasMessageContaining("immuable");
    }
    @Test void oldWriterInitializesOrChecksAndExplicitPondererChangeCascades() throws Exception {
        seed(); flyway(null).migrate();
        sql("INSERT INTO enseignement(id_enseignement,id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut) VALUES (5,1,99,28,4,'2026-09-01')");
        assertThat(scalar("SELECT coefficient_matiere FROM ponderer WHERE id_classe=99")).isEqualTo("4.00");
        assertThatThrownBy(() -> sql("UPDATE enseignement SET coefficient_matiere=3 WHERE id_enseignement=1"))
                .hasMessageContaining("modifier PONDERER explicitement");
        assertThatThrownBy(() -> sql("UPDATE enseignement SET id_classe=99 WHERE id_enseignement=1"))
                .hasMessageContaining("coefficient différent");
        sql("UPDATE ponderer SET coefficient_matiere=5 WHERE id_classe=25 AND id_matiere=28");
        assertThat(scalar("SELECT count(*) FROM enseignement WHERE id_classe=25 AND coefficient_matiere=5")).isEqualTo("2");
        sql("UPDATE enseignement SET id_classe=99,coefficient_matiere=4 WHERE id_enseignement=4");
        assertConsistent();
    }
    @Test void failedOldCreationRollsBackItsPondererInitialization() throws Exception {
        seed(); flyway(null).migrate();
        assertThatThrownBy(() -> sql("INSERT INTO enseignement(id_enseignement,id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut) VALUES (5,9999,99,28,4,'2026-09-01')"))
                .isInstanceOf(SQLException.class);
        assertThat(scalar("SELECT count(*) FROM ponderer WHERE id_classe=99")).isEqualTo("0");
    }
    @ParameterizedTest @ValueSource(strings = {"4", "7"})
    void concurrentFirstCreationsSerializeAndCheckCoefficient(String secondCoefficient) throws Exception {
        seed(); flyway(null).migrate();
        try (Connection first = connection(); Connection second = connection();
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            first.setAutoCommit(false);
            try (Statement st = first.createStatement()) {
                st.execute("INSERT INTO enseignement(id_enseignement,id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut) VALUES (5,1,99,28,4,'2026-09-01')");
            }
            int secondPid;
            try (Statement st = second.createStatement(); ResultSet rs = st.executeQuery("SELECT pg_backend_pid()")) {
                rs.next(); secondPid = rs.getInt(1);
            }
            CountDownLatch started = new CountDownLatch(1);
            Future<String> loser = executor.submit(() -> {
                started.countDown();
                try (Statement st = second.createStatement()) {
                    st.execute("INSERT INTO enseignement(id_enseignement,id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut) VALUES (6,1,99,28," + secondCoefficient + ",'2026-10-01')");
                    return "success";
                } catch (SQLException e) { return e.getSQLState(); }
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            boolean waiting = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                if ("Lock".equals(scalar("SELECT wait_event_type FROM pg_stat_activity WHERE pid=" + secondPid))) {
                    waiting = true;
                    break;
                }
                Thread.sleep(20);
            }
            assertThat(waiting).as("le deuxième écrivain attend réellement le verrou du premier").isTrue();
            first.commit();
            assertThat(loser.get(10, TimeUnit.SECONDS)).isEqualTo(secondCoefficient.equals("4") ? "success" : "23514");
        }
        assertConsistent();
        assertThat(scalar("SELECT coefficient_matiere FROM ponderer WHERE id_classe=99")).isEqualTo("4.00");
    }
    @Test void referenceLockBeforeTeachingWriteSerializesConcurrentCoefficientUpdate() throws Exception {
        seed(); flyway(null).migrate();
        try (Connection teaching = connection(); Connection weighting = connection();
             ExecutorService executor = Executors.newSingleThreadExecutor()) {
            teaching.setAutoCommit(false);
            try (Statement st = teaching.createStatement()) {
                st.execute("SELECT * FROM ponderer WHERE id_classe=25 AND id_matiere=28 FOR KEY SHARE");
            }
            int weightingPid;
            try (Statement st = weighting.createStatement(); ResultSet rs = st.executeQuery("SELECT pg_backend_pid()")) {
                rs.next(); weightingPid = rs.getInt(1);
            }
            Future<Integer> updated = executor.submit(() -> {
                try (Statement st = weighting.createStatement()) {
                    return st.executeUpdate("UPDATE ponderer SET coefficient_matiere=5 WHERE id_classe=25 AND id_matiere=28");
                }
            });
            boolean waiting = false;
            for (int attempt=0; attempt<100; attempt++) {
                if ("Lock".equals(scalar("SELECT wait_event_type FROM pg_stat_activity WHERE pid=" + weightingPid))) {
                    waiting=true; break;
                }
                Thread.sleep(20);
            }
            assertThat(waiting).isTrue();
            try (Statement st = teaching.createStatement()) {
                st.executeUpdate("UPDATE enseignement SET coefficient_matiere=2,date_fin='2027-06-30' WHERE id_enseignement=1");
            }
            teaching.commit();
            assertThat(updated.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertConsistent();
        assertThat(scalar("SELECT count(*) FROM enseignement WHERE id_classe=25 AND coefficient_matiere=5")).isEqualTo("2");
    }

}
