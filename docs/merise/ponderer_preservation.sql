-- Même requête avant/après V2, tous les écrivains métier suspendus.
-- Sortie : uniquement volumes et empreintes, aucun contenu personnel.
\set ON_ERROR_STOP on
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL statement_timeout = '15s';
SET LOCAL lock_timeout = '3s';
SELECT format(
    'SELECT %L AS table_metier,count(*) AS lignes,md5(coalesce(string_agg(to_jsonb(t)::text,chr(10) ORDER BY to_jsonb(t)::text),'''')) AS empreinte FROM %I t;',
    table_name,table_name)
FROM unnest(ARRAY['personne','compte_utilisateur','eleve','enseignant','responsable',
    'classe','matiere','periode','inscription','scolarite','enseignement','evaluation',
    'note','bulletin','ligne_bulletin','responsabilite_legale']) AS tables(table_name)
ORDER BY table_name \gexec
COMMIT;
