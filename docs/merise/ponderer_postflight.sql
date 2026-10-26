-- Lecture seule après V2. La conservation des 16 tables se contrôle séparément.
\set ON_ERROR_STOP on
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL statement_timeout = '15s';
SET LOCAL lock_timeout = '3s';
SELECT current_database(), current_setting('server_version'), current_setting('transaction_read_only');
SELECT version,checksum,success FROM flyway_schema_history ORDER BY installed_rank;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version='2' AND success) THEN
        RAISE EXCEPTION 'V2 réussie absente de Flyway';
    END IF;
    IF EXISTS (
        SELECT 1 FROM enseignement e LEFT JOIN ponderer p USING(id_classe,id_matiere)
        WHERE p.id_classe IS NULL OR e.coefficient_matiere IS DISTINCT FROM p.coefficient_matiere
    ) THEN
        RAISE EXCEPTION 'PONDERER : couverture ou égalité du miroir incorrecte';
    END IF;
    IF EXISTS (SELECT 1 FROM ponderer WHERE coefficient_matiere<=0 OR coefficient_matiere='NaN'::numeric) THEN
        RAISE EXCEPTION 'PONDERER : coefficient invalide';
    END IF;
    IF (SELECT count(*) FROM ponderer) <> (SELECT count(*) FROM (SELECT id_classe,id_matiere FROM enseignement GROUP BY 1,2) x) THEN
        RAISE EXCEPTION 'Nombre de pondérations différent du nombre de couples repris';
    END IF;
    IF (SELECT count(*) FROM pg_constraint WHERE connamespace='public'::regnamespace AND convalidated
        AND conname IN ('pk_ponderer','fk_ponderer_classe','fk_ponderer_matiere','ck_ponderer_coefficient',
                        'uq_ponderer_miroir','fk_enseignement_ponderer_miroir')) <> 6 THEN
        RAISE EXCEPTION 'Contraintes PONDERER absentes ou non validées';
    END IF;
    IF (SELECT count(*) FROM pg_trigger WHERE NOT tgisinternal AND tgenabled='O'
        AND tgrelid IN ('public.ponderer'::regclass,'public.enseignement'::regclass)
        AND tgname IN ('tr_enseignement_ponderer','tr_ponderer_cle_immuable')) <> 2 THEN
        RAISE EXCEPTION 'Triggers de transition absents ou désactivés';
    END IF;
END $$;
SELECT id_classe,id_matiere,coefficient_matiere FROM ponderer ORDER BY 1,2;
COMMIT;
