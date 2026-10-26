-- Lecture seule, compatible V1. À rejouer après suspension des écrivains.
\set ON_ERROR_STOP on
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL statement_timeout = '15s';
SET LOCAL lock_timeout = '3s';
SELECT current_database(), current_setting('server_version'), current_setting('transaction_read_only');
SELECT version, checksum, success FROM flyway_schema_history ORDER BY installed_rank;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='1' AND checksum=-526686782 AND success)
       OR EXISTS (SELECT 1 FROM flyway_schema_history WHERE version IS NOT NULL AND version<>'1') THEN
        RAISE EXCEPTION 'Précondition : historique Flyway V1 seule, checksum attendu et succès requis';
    END IF;
    IF EXISTS (
        SELECT 1 FROM enseignement GROUP BY id_classe,id_matiere
        HAVING count(DISTINCT coefficient_matiere)<>1
            OR bool_or(coefficient_matiere IS NULL OR coefficient_matiere<=0 OR coefficient_matiere='NaN'::numeric)
    ) THEN
        RAISE EXCEPTION 'PONDERER : coefficient invalide ou contradictoire ; résolution métier nécessaire';
    END IF;
    IF to_regclass('public.ponderer') IS NOT NULL THEN
        RAISE EXCEPTION 'PONDERER existe déjà ; vérifier le schéma avant toute intervention';
    END IF;
END $$;
SELECT id_classe,id_matiere,count(*) AS enseignements,
       count(DISTINCT coefficient_matiere) AS valeurs_distinctes,
       min(coefficient_matiere) AS coefficient
FROM enseignement GROUP BY id_classe,id_matiere ORDER BY 1,2;
SELECT count(*) AS ponderations_attendues
FROM (SELECT id_classe,id_matiere FROM enseignement GROUP BY 1,2) groupes;
COMMIT;
