-- Lot 1 : PONDERER devient la référence ; la colonne V1 reste un miroir.
-- Flyway exécute ce fichier dans une transaction PostgreSQL.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
LOCK TABLE enseignement IN SHARE ROW EXCLUSIVE MODE;

DO $$
DECLARE
    conflit RECORD;
BEGIN
    SELECT id_classe, id_matiere INTO conflit
    FROM enseignement
    GROUP BY id_classe, id_matiere
    HAVING count(DISTINCT coefficient_matiere) <> 1
        OR bool_or(coefficient_matiere IS NULL OR coefficient_matiere <= 0
                   OR coefficient_matiere = 'NaN'::numeric)
    ORDER BY id_classe, id_matiere
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'PONDERER : coefficients incohérents pour classe %, matière % ; reprise annulée',
            conflit.id_classe, conflit.id_matiere;
    END IF;
END $$;

CREATE TABLE ponderer (
    id_classe BIGINT NOT NULL,
    id_matiere BIGINT NOT NULL,
    coefficient_matiere NUMERIC(5,2) NOT NULL,
    CONSTRAINT pk_ponderer PRIMARY KEY (id_classe, id_matiere),
    CONSTRAINT fk_ponderer_classe FOREIGN KEY (id_classe) REFERENCES classe(id_classe),
    CONSTRAINT fk_ponderer_matiere FOREIGN KEY (id_matiere) REFERENCES matiere(id_matiere),
    CONSTRAINT ck_ponderer_coefficient CHECK (coefficient_matiere > 0 AND coefficient_matiere <> 'NaN'::numeric),
    -- Clé technique transitoire pour garantir l'égalité du miroir V1 par FK.
    CONSTRAINT uq_ponderer_miroir UNIQUE (id_classe, id_matiere, coefficient_matiere)
);

-- MIN ne sélectionne pas une valeur arbitraire : l'unicité a été vérifiée sous verrou.
INSERT INTO ponderer (id_classe, id_matiere, coefficient_matiere)
SELECT id_classe, id_matiere, min(coefficient_matiere)
FROM enseignement GROUP BY id_classe, id_matiere;

ALTER TABLE enseignement ADD CONSTRAINT fk_enseignement_ponderer_miroir
    FOREIGN KEY (id_classe, id_matiere, coefficient_matiere)
    REFERENCES ponderer(id_classe, id_matiere, coefficient_matiere)
    ON UPDATE CASCADE ON DELETE RESTRICT;

-- Un ancien écrivain peut initialiser un nouveau couple. Sur un couple existant,
-- sa valeur doit être identique : aucune modification implicite du coefficient commun.
CREATE FUNCTION initialiser_ponderer_enseignement() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    coefficient_reference NUMERIC(5,2);
BEGIN
    INSERT INTO ponderer (id_classe, id_matiere, coefficient_matiere)
    VALUES (NEW.id_classe, NEW.id_matiere, NEW.coefficient_matiere)
    ON CONFLICT (id_classe, id_matiere) DO NOTHING;

    SELECT coefficient_matiere INTO coefficient_reference FROM ponderer
    WHERE id_classe = NEW.id_classe AND id_matiere = NEW.id_matiere
    FOR KEY SHARE;
    IF NOT FOUND OR coefficient_reference IS DISTINCT FROM NEW.coefficient_matiere THEN
        RAISE EXCEPTION 'PONDERER : coefficient différent pour classe %, matière % ; modifier PONDERER explicitement',
            NEW.id_classe, NEW.id_matiere USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER tr_enseignement_ponderer
    BEFORE INSERT OR UPDATE OF id_classe, id_matiere, coefficient_matiere ON enseignement
    FOR EACH ROW EXECUTE FUNCTION initialiser_ponderer_enseignement();

-- La cascade ne doit jamais déplacer les enseignements vers un autre couple.
CREATE FUNCTION proteger_cle_ponderer() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id_classe, NEW.id_matiere) IS DISTINCT FROM (OLD.id_classe, OLD.id_matiere) THEN
        RAISE EXCEPTION 'PONDERER : la clé classe-matière est immuable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER tr_ponderer_cle_immuable BEFORE UPDATE OF id_classe, id_matiere ON ponderer
    FOR EACH ROW EXECUTE FUNCTION proteger_cle_ponderer();
