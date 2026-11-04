package fr.afpa.backend.migration;

import fr.afpa.backend.dto.periode.PeriodeRequest;
import fr.afpa.backend.support.IsolatedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class PeriodeAnneeScolaireMigrationTest {
    private String schema;
    private Connection db;

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(IsolatedPostgres.url(), "ponderer_test", "ponderer_test")
                .schemas(schema).defaultSchema(schema).target(target).locations("classpath:db/migration").load();
    }

    @BeforeEach void prepare() throws Exception {
        schema = "periode_" + UUID.randomUUID().toString().replace("-", "");
        flyway("2").migrate();
        db = IsolatedPostgres.connect();
        db.setSchema(schema);
        sql("SET statement_timeout='10s'; SET lock_timeout='5s'");
    }

    @AfterEach void cleanup() throws Exception {
        if (db != null) db.close();
        if (schema != null) try (Connection c = IsolatedPostgres.connect(); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    private void sql(String command) throws Exception {
        try (Statement st = db.createStatement()) { st.execute(command); }
    }

    private String scalar(String query) throws Exception {
        try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery(query)) {
            rs.next(); return rs.getString(1);
        }
    }

    private Map<String, String> snapshot() throws Exception {
        Map<String, String> out = new TreeMap<>();
        List<String> tables = new ArrayList<>();
        try (var st = db.prepareStatement("SELECT tablename FROM pg_tables WHERE schemaname=? AND tablename<>'flyway_schema_history' ORDER BY tablename")) {
            st.setString(1, schema);
            try (var rs = st.executeQuery()) { while (rs.next()) tables.add(rs.getString(1)); }
        }
        for (String t : tables) {
            String row = t.equals("periode") ? "to_jsonb(t)-'annee_scolaire'" : "to_jsonb(t)";
            out.put(t, scalar("SELECT coalesce(jsonb_agg(r ORDER BY r::text)::text,'[]') FROM (SELECT " + row + " AS r FROM " + t + " t) x"));
        }
        try (var st = db.prepareStatement("SELECT sequencename FROM pg_sequences WHERE schemaname=? ORDER BY sequencename")) {
            st.setString(1, schema);
            List<String> sequences = new ArrayList<>();
            try (var rs = st.executeQuery()) { while (rs.next()) sequences.add(rs.getString(1)); }
            for (String seq : sequences) out.put("sequence/" + seq, scalar("SELECT json_build_object('last_value',last_value,'is_called',is_called)::text FROM " + seq));
        }
        out.put("constraints", scalar("""
                SELECT jsonb_agg(x ORDER BY x::text)::text FROM (
                  SELECT jsonb_build_array(c.relname,con.conname,con.contype,pg_get_constraintdef(con.oid)) AS x
                  FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid
                  WHERE c.relnamespace=current_schema()::regnamespace
                    AND con.conname<>'ck_periode_annee_scolaire'
                ) z
                """));
        return out;
    }

    private void seed() throws Exception {
        sql("""
                INSERT INTO personne(id_personne,nom,prenom) VALUES (1,'Test','Prof'),(2,'Test','Elève');
                INSERT INTO enseignant VALUES (1,'TEST-3A');
                INSERT INTO eleve(id_personne,matricule,date_naissance) VALUES (2,'TEST-3A','2014-01-02');
                INSERT INTO classe(id_classe,nom,niveau,annee_scolaire) VALUES (1,'Test','6','2026-2027');
                INSERT INTO matiere(id_matiere,code,nom) VALUES (1,'TEST','Maths');
                INSERT INTO enseignement(id_enseignement,id_enseignant,id_classe,id_matiere,coefficient_matiere,date_debut)
                  VALUES (1,1,1,1,2,'2026-09-01');
                INSERT INTO scolarite(id_scolarite,id_eleve,id_classe,date_debut,statut) VALUES (1,2,1,'2026-09-01','ACTIVE');
                INSERT INTO periode(id_periode,libelle,date_debut,date_fin,statut) VALUES
                  (17,'Historique 17','2026-09-01','2026-12-31','OUVERTE'),
                  (20,'Historique 20','2026-09-01','2026-12-31','OUVERTE'),
                  (28,'Historique 28','2026-09-01','2026-12-31','OUVERTE');
                INSERT INTO evaluation(id_evaluation,id_enseignement,id_periode,libelle,date_evaluation,type_evaluation)
                  VALUES (1,1,17,'Test','2026-10-20','DEVOIR'),(2,1,20,'Test','2026-10-20','DEVOIR'),(3,1,28,'Test','2026-10-20','DEVOIR');
                INSERT INTO note(id_scolarite,id_evaluation,valeur,statut_note,date_saisie)
                  VALUES (1,1,12,'SAISIE','2026-10-03T20:00:00Z'),(1,2,13,'SAISIE','2026-10-04T08:00:00Z'),(1,3,14,'VALIDEE','2026-10-04T08:00:00Z');
                INSERT INTO bulletin(id_bulletin,id_scolarite,id_periode,statut) VALUES (1,1,17,'BROUILLON');
                INSERT INTO ligne_bulletin(id_bulletin,id_enseignement,texte,statut) VALUES (1,1,'À conserver','BROUILLON');
                SELECT nextval('periode_id_periode_seq');
                """);
    }

    @Test void freshV1V2V3InstallationAndNullableColumnWithoutDefault() throws Exception {
        // A second, empty schema verifies all three migrations in a single call.
        String base = schema;
        schema = base + "_fresh";
        try {
            assertThat(flyway("3").migrate().migrationsExecuted).isEqualTo(3);
            flyway("3").validate();
        } finally {
            try (var st = db.createStatement()) { st.execute("DROP SCHEMA " + schema + " CASCADE"); }
            schema = base;
        }
        flyway("3").migrate();
        assertThat(scalar("SELECT is_nullable||':'||character_maximum_length::text||':'||coalesce(column_default,'NO_DEFAULT') FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='periode' AND column_name='annee_scolaire'"))
                .isEqualTo("YES:9:NO_DEFAULT");
        assertThat(scalar("SELECT data_type FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='periode' AND column_name='annee_scolaire'"))
                .isEqualTo("character varying");
    }

    @Test void v2ToV3PreservesEveryOriginalValueSequenceAndConstraint() throws Exception {
        seed();
        var before = snapshot();
        assertThat(flyway("3").migrate().migrationsExecuted).isEqualTo(1);
        assertThat(snapshot()).isEqualTo(before);
        assertThat(scalar("SELECT count(*) FROM periode WHERE id_periode IN (17,20,28) AND annee_scolaire IS NULL")).isEqualTo("3");
        assertThat(scalar("SELECT count(*) FROM inscription")).isEqualTo("0");
        assertThat(scalar("SELECT count(*) FROM note n JOIN evaluation e USING(id_evaluation) WHERE n.date_saisie::date<e.date_evaluation")).isEqualTo("3");
        assertThat(scalar("SELECT count(*) FROM periode a JOIN periode b ON a.id_periode<b.id_periode AND a.date_debut<=b.date_fin AND b.date_debut<=a.date_fin")).isEqualTo("3");
    }

    @Test void secondFlywayExecutionDoesNotReapplyV3() throws Exception {
        seed();
        flyway("3").migrate();
        var before = snapshot();
        assertThat(flyway("3").migrate().migrationsExecuted).isZero();
        assertThat(snapshot()).isEqualTo(before);
        assertThat(scalar("SELECT count(*) FROM flyway_schema_history WHERE version='3' AND success")).isEqualTo("1");
    }

    private boolean javaAccepts(String value) {
        return new PeriodeRequest("Test", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), null, null, "OUVERTE", value)
                .isAnneeScolaireValide();
    }

    @Test void sqlAndJavaAcceptSameCanonicalValuesAndNull() throws Exception {
        seed(); flyway("3").migrate();
        for (String year : Arrays.asList(null, "0000-0001", "0001-0002", "0999-1000", "2026-2027", "9998-9999")) {
            assertThat(javaAccepts(year)).as("Java %s", year).isTrue();
            try (var st = db.prepareStatement("UPDATE periode SET annee_scolaire=? WHERE id_periode=17")) {
                st.setString(1, year); st.executeUpdate();
            }
            assertThat(scalar("SELECT annee_scolaire FROM periode WHERE id_periode=17")).isEqualTo(year);
        }
    }

    @Test void sqlAndJavaRejectMalformedOrNonConsecutiveYears() throws Exception {
        seed(); flyway("3").migrate();
        for (String year : List.of("", " ", "2026", "26-27", "2026/2027", "2026-2028", "2026-2026", "2027-2026",
                "9999-0000", "0000-0000", "２０２６-２０２７", "202a-2027", "2026-202x", "2026-20 7", "2026-2027\n", "2026-20270")) {
            assertThat(javaAccepts(year)).as("Java %s", year).isFalse();
            try (var st = db.prepareStatement("UPDATE periode SET annee_scolaire=? WHERE id_periode=17")) {
                st.setString(1, year);
                assertThatThrownBy(st::executeUpdate).as("SQL %s", year).isInstanceOf(SQLException.class)
                        .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isIn("23514", "22001"));
            }
            assertThat(scalar("SELECT annee_scolaire FROM periode WHERE id_periode=17")).isNull();
        }
    }

    @Test void documentsVarcharSpaceCoercionWhileStoredValueStillMatchesJava() throws Exception {
        seed(); flyway("3").migrate();
        // PostgreSQL VARCHAR(n) discards surplus trailing spaces before CHECK evaluation.
        // The HTTP/Java boundary rejects the original noncanonical input.
        assertThat(javaAccepts("2026-2027 ")).isFalse();
        sql("UPDATE periode SET annee_scolaire='2026-2027 ' WHERE id_periode=17");
        String stored = scalar("SELECT annee_scolaire FROM periode WHERE id_periode=17");
        assertThat(stored).isEqualTo("2026-2027");
        assertThat(javaAccepts(stored)).isTrue();
    }

    @Test void originalMigrationChecksumsAreUnchanged() throws Exception {
        var expected = Map.of("V1__create_initial_schema.sql", "f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee",
                "V2__create_ponderer.sql", "35734eef70167a37c7f97749f364b88bdd10b6c7396df1c8a36e41548adb25f9");
        for (var entry : expected.entrySet()) {
            try (var stream = getClass().getResourceAsStream("/db/migration/" + entry.getKey())) {
                assertThat(stream).isNotNull();
                assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes())))
                        .isEqualTo(entry.getValue());
            }
        }
    }
}
