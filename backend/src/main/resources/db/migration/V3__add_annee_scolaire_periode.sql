-- Extension uniquement : aucune reprise des années des périodes historiques.
ALTER TABLE periode
    ADD COLUMN annee_scolaire VARCHAR(9),
    ADD CONSTRAINT ck_periode_annee_scolaire
        CHECK (
            CASE
                WHEN annee_scolaire IS NULL THEN TRUE
                WHEN char_length(annee_scolaire) = 9
                    AND annee_scolaire ~ '^[0-9]{4}-[0-9]{4}$'
                THEN substring(annee_scolaire FROM 6 FOR 4)::INTEGER
                    = substring(annee_scolaire FROM 1 FOR 4)::INTEGER + 1
                ELSE FALSE
            END
        );
