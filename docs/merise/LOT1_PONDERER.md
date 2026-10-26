# Lot 1 — PONDERER

Branche : `feature/merise-v2-ponderer`. Aucun commit ni push. V1 et l'audit restent inchangés. La validation finale du 26 octobre 2026 a accédé à l'original uniquement en lecture seule pour sa sauvegarde et ses contrôles. Aucune migration, aucun backend et aucun test n'ont ciblé cette base. Les démarrages ont utilisé une copie restaurée ou des fixtures isolées.

## Modèle et reprise

`Ponderer` porte une identité `PondererId(idClasse,idMatiere)` avec `@EmbeddedId`, deux associations `@ManyToOne(fetch=LAZY) @MapsId` vers Classe et Matiere, sans cascade de suppression, et un coefficient `NUMERIC(5,2)` obligatoire. La PK composée assure l'unicité du couple. Le CHECK impose une valeur strictement positive et exclut aussi `NaN`, que PostgreSQL autorise dans NUMERIC.

V2 verrouille les écritures d'enseignement avant de contrôler **tous** les enseignements, actifs et historiques. Un couple comportant plusieurs coefficients, NULL, une valeur non positive ou NaN fait échouer explicitement la migration et sa transaction. Le couple en cause est indiqué sans identité personnelle. MIN est utilisé uniquement après preuve d'une valeur unique sous verrou. Aucun produit cartésien, défaut de coefficient ou modification des lignes V1. La FK ajoutée valide la couverture et l'égalité de toutes les lignes reprises. Délais locaux : verrouillage 5 secondes, instruction 60 secondes.

Les trois couples de l'audit sont reproduits par une fixture anonyme : `(25,28)`, `(30,32)`, `(42,41)` à 2.00 ; un enseignement historique supplémentaire du premier couple reste conservé et n'ajoute pas de pondération. Ce test ne constitue pas une restauration des données réelles.

Les tables/entités Bulletin et LigneBulletin persistées, l'héritage Java, les habilitations et le cycle de validation restent inchangés.

## Écriture et compatibilité

PONDERER est la référence. `enseignement.coefficient_matiere` reste un miroir transitoire et garde son contrat JSON. Les règles suivantes s'appliquent également aux enseignements inactifs :

| Action | Comportement |
|---|---|
| Création d'un enseignement, couple absent | Initialisation explicite depuis le coefficient du DTO, dans la même transaction |
| Création/modification, couple présent | Coefficient numériquement identique exigé ; 2 et 2.00 sont équivalents ; conflit HTTP 409 sinon |
| Changement de classe/matière d'un enseignement | Initialisation/vérification du couple cible ; pondération source conservée |
| Modification du coefficient commun | Uniquement via PUT `/ponderations/{idClasse}/{idMatiere}` ; cascade SQL vers **tous** les miroirs du couple |
| Modification des identifiants de PONDERER | Refusée ; la clé est immuable |
| Suppression d'une pondération utilisée | Refusée par le service et la FK SQL |
| Suppression du dernier enseignement | La pondération reste une configuration de classe/matière |
| Suppression d'une classe/matière pondérée | Refus métier explicite, complété par les FK |

Une FK transitoire sur `(id_classe,id_matiere,coefficient_matiere)` référence une UNIQUE technique de PONDERER avec `ON UPDATE CASCADE ON DELETE RESTRICT`. Le trigger d'enseignement initialise le couple si nécessaire puis vérifie sa valeur sous verrou ; il ne choisit jamais une valeur concurrente silencieusement. `ON CONFLICT DO NOTHING` est toujours suivi d'une vérification explicite. Un ancien processus ou une écriture SQL ordinaire ne peut donc pas créer une valeur divergente. Les anciens appels voulant changer un coefficient localement sont volontairement refusés ; ils doivent passer par PONDERER.

Le service de pondération utilise une mise à jour atomique et vide le contexte JPA après la cascade pour éviter de relire un ancien miroir déjà chargé. Avant de modifier un enseignement, `ensureCompatible` verrouille la ligne PONDERER cible en `FOR KEY SHARE`, puis vérifie son coefficient. L'ordre est donc référence → enseignement, cohérent avec la cascade. Un test à deux connexions observe l'attente de la modification concurrente du coefficient, puis vérifie que les deux écritures réussissent sans divergence. Les créations/modifications d'enseignements sont flushées avant de produire la réponse. Une violation SQL concurrente renvoie un conflit HTTP 409 générique sans détail SQL ni données personnelles. Une défaillance de concurrence (dont conflit de verrouillage) est aussi traduite en HTTP 409, sans détail SQL. Une transaction concurrente annulée doit être rejouée après relecture ; aucun mécanisme de dernière-écriture datée ou d'historisation des coefficients n'est ajouté.

API d'administration : GET liste, GET couple, POST, PUT couple, DELETE couple sous `/ponderations`, réservés à ADMIN, y compris les lectures. Le POST renvoie une Location à deux identifiants. DTO validés : identifiants positifs et coefficient positif, au maximum trois chiffres entiers/deux décimales. Le frontend propose Pondérations dans le tableau de bord ADMIN, verrouille les clés en modification et explique la règle dans Enseignements.

## Calcul des bulletins

Le calcul lit PONDERER par classe et agrège les **notes** de tous les enseignements pertinents d'une même matière. Le coefficient de matière est appliqué une seule fois. Les coefficients d'évaluation et les barèmes restent respectés ; aucune moyenne simple de moyennes d'enseignants.

Conservation des règles existantes : intersection des dates enseignement/période, absence de filtre nouveau sur actif ou statut de note, calcul à 10 décimales, affichage HALF_UP à 2, exclusion des matières sans moyenne du dénominateur général. Une pondération absente/invalide produit une erreur explicite, même sans note. Aucun coefficient 1 implicite.

Le DTO calculé conserve `idEnseignement` pour compatibilité : c'est le plus petit identifiant du groupe, sans attribution exclusive de la ligne à cet enseignant. `idMatiere` identifie la ligne, utilisé aussi comme clé React. Les routes de calcul/PDF et la génération PDF existantes sont conservées.

## Vérifications exécutées

- `./scripts/test-ponderer.sh -o` : suite Java complète sur PostgreSQL **17.10** jetable, **361 tests réussis**, zéro échec, erreur ou test ignoré. Le script a été exécuté avec succès avec un port Docker attribué automatiquement ; il supprime uniquement le conteneur qu'il crée.
- Tests de migration : V1 vide → V2 ; reprise non vide incluant un enseignement inactif ; égalité des valeurs et relations de toutes les 16 tables d'origine, y compris un bulletin et une ligne persistés ; préservation de V1 par SHA-256 ; conflits historiques et coefficients invalides avec rollback ; PK/FK/CHECK ; écriture ancienne, déplacements et cascade ; deux transactions réellement en concurrence (attente de verrou observée), coefficients identiques et différents.
- Intégration Spring/JPA/HTTP : `ddl-auto=validate`, `@MapsId`, flush/clear/relecture, absence de cascade vers les parents, écritures d'enseignement, miroir rafraîchi, conflits, validation DTO, Location composée, refus 401/403, CRUD ADMIN.
- Unitaires : nouveaux cas de calcul multi-enseignants, source PONDERER et coefficient manquant ; service de pondération ; suite existante conservée.
- `npm --prefix frontend test` : **12 tests réussis** ; `npm --prefix frontend run lint` et `npm --prefix frontend run build` : succès.
- `git diff --check` et `bash -n scripts/test-ponderer.sh` : succès.
- V1 SHA-256 : `f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee`.

L'isolation est obligatoire pour les tests Spring : base `ponderer_test`, utilisateur dédié, URL explicite 127.0.0.1 sur un port autre que 5432, propriétés datasource **et Flyway** forcées par `IsolatedPostgres`, bootstrap neutralisé. Sans variable de test valide, les tests de contexte échouent avant toute connexion. La configuration de tests ne reprend pas DB_URL. Les tests unitaires seuls restent exécutables hors ligne avec Maven.

## Validation finale sur les données réelles — 26 octobre 2026

**Résultat : lot prêt pour une revue finale avant commit.** Aucun test échoué ne reste à corriger.

### Sauvegarde et restauration exécutées

Sauvegarde logique `pg_dump --serializable-deferrable -Fc` avec `PGOPTIONS` imposant `default_transaction_read_only=on`, depuis le conteneur source PostgreSQL **17.10**. Archive conservée, droits de répertoire 0700, ignorée par Git :

`/home/florian/gestion-eleves/.validation-backups/ponderer-3hshPLxS/gestion_eleves-v1.dump`

SHA-256 : `16f8b78437f6be78e1204de70efe200eda538d77ebb6ddf16acf3c7e9b40939f`.

Restauration réussie avec `pg_restore --exit-on-error` dans `gestion-eleves-ponderer-copy-jnzq1y7j`, PostgreSQL **17.10**, port hôte **55436**, volume neuf `gestion-eleves-ponderer-copy-volume-jnzq1y7j`. Aucun montage du volume original. Comparaison immédiate : égalité des 16 tables métier avec la source sauvegardée.

Flyway **11.7.2**, cible explicite 2 : **une migration appliquée**, V1 → V2, puis validation réussie. V2 checksum `1440610697` ; V1 checksum `-526686782`. Le précontrôle `validate` initial a révélé qu'une migration pending doit être autorisée explicitement : la procédure a été corrigée avec `ignoreMigrationPatterns=*:pending` avant migration. Aucun DDL n'avait été exécuté lors de ce premier arrêt de validation.

| Couple repris | Coefficient |
|---|---:|
| classe 25 / matière 28 | 2.00 |
| classe 30 / matière 32 | 2.00 |
| classe 42 / matière 41 | 2.00 |

Zéro couple manquant, zéro divergence de miroir, six contraintes attendues validées et deux triggers applicatifs actifs. Les empreintes de **toutes les 16 tables V1** restent identiques après migration et après les lectures HTTP Spring Boot. En particulier :

| Table | Avant restauration/migration | Après V2 et contrôles HTTP | Contenu |
|---|---:|---:|---|
| enseignement | 3 | 3 | identique |
| scolarite | 3 | 3 | identique |
| evaluation | 3 | 3 | identique |
| note | 3 | 3 | identique |

### Spring Boot sur la copie migrée

Démarrage du JAR final réussi sur HTTP **18086**, datasource et Flyway explicitement configurés sur **55436**, bootstrap entièrement neutralisé, validation Hibernate du schéma conservée. GET authentifiés réussis sur enseignements, scolarités, évaluations, notes, pondérations, matières et classes. Les trois bulletins des données réelles ont été calculés, comparés à un calcul SQL indépendant adapté aux données présentes, et leurs trois PDF vérifiés (HTTP 200, `application/pdf`, signature `%PDF-`, contenu non vide).

Pour ces lectures, un jeton de test a été signé avec une clé temporaire propre à cette instance et rattaché au compte ADMIN restauré. Aucun compte, mot de passe ou hash n'a été modifié. La connexion via formulaire et les droits réels sont couverts séparément par la recette de fixtures ci-dessous. Aucun contenu nominatif ni jeton n'est reproduit dans ce rapport.

### Recette complète isolée

- `./scripts/test-ponderer.sh -o` : **361 réussites**, zéro échec/erreur/ignoré.
- `npm --prefix frontend test` : **12 réussites**.
- `npm --prefix frontend run lint` et `npm --prefix frontend run build` : **succès**.
- `./scripts/test-e2e.sh` : **12 scénarios Chromium réussis**, zéro échec final, dont rôles ENSEIGNANT/RESPONSABLE et toutes les pages d'administration. Dernière exécution : **38,3 secondes** pour Playwright.
- `git diff --check`, `bash -n` des deux lanceurs et validation syntaxique du workflow CI : **succès**.

Playwright utilise une base neuve `ponderer_e2e` dans son propre conteneur, des comptes de fixtures et des ports alloués au lancement. Les API des scénarios passent par `/api` sur le frontend dédié, dont le proxy cible explicitement le backend dédié. La configuration refuse un lancement sans cibles isolées et refuse les ports frontend/backend de développement. Aucun scénario navigateur ne s'est exécuté sur la copie des données personnelles ou sur l'original.

Le parcours Enseignements vérifie maintenant : création du couple, refus HTTP 409 d'un coefficient local divergent sans modification partielle, refus de supprimer la pondération utilisée, modification du coefficient via Pondérations, cascade visible dans l'API enseignement, clés non modifiables dans l'écran, modification des autres attributs et suppression de l'enseignement.

Le parcours Bulletin utilise deux enseignements d'une même matière : notes 15/20 coefficient 2 et 4.5/10 coefficient 1 ; moyenne attendue **13/20**, une seule ligne, deux notes, coefficient de matière 2 appliqué une seule fois. Vérification du résultat JSON, du rendu et du téléchargement PDF. Les pondérations multi-matières sont aussi couvertes en tests Java.

### Corrections pendant cette validation

1. Scénario historique d'enseignement : le premier passage a donné **11 réussites / 1 échec**, car il attendait encore une modification locale du coefficient. Le contrat PONDERER est désormais vérifié par un refus, puis une modification explicite de la référence. Un second passage a révélé un sélecteur exact d'étiquette ambigu sur les selects ; il utilise maintenant le rôle accessible `combobox`. Les deux derniers passages complets sont réussis.
2. Concurrence : ajout du verrou partagé sur la référence avant le flush d'enseignement et traduction HTTP 409 des échecs de concurrence ; deux contrôles supplémentaires expliquent le passage de 359 à 361 tests Java.
3. Exécution E2E : remplacement de la réutilisation implicite de Compose/8080/5173 par un lanceur qui crée et nettoie uniquement ses propres ressources. La CI emploie ces mêmes lanceurs ; les consignes README ont été mises à jour.
4. Lint après Playwright : les fichiers JavaScript générés dans le rapport produisaient des erreurs de dépendances générées. Les rapports/résultats sont exclus et `vite.config.js` reçoit les globals Node nécessaires à la configuration du proxy. Le lint final est réussi.
5. Procédure Flyway : autorisation explicite des migrations pending seulement au précontrôle, avant application de V2 sur copie.

### Revue technique et risques résiduels

Les 44 fichiers initiaux ont été revus, ainsi que les corrections et les fichiers d'infrastructure ajoutés à ce diff. PONDERER est l'unique référence métier ; aucun calcul ne lit le miroir V1. La UNIQUE technique à trois colonnes est nécessaire à la FK d'égalité ; elle n'introduit pas une seconde identité métier. Les deux triggers couvrent l'initialisation/vérification des anciens écrivains et l'immuabilité de la clé ; la FK assure la cascade et protège la suppression. Aucun nouvel index, verrou global applicatif ou mécanisme de retry n'a été ajouté.

Les écritures de l'application verrouillent la référence avant l'enseignement. Des écrivains SQL anciens ou des transactions touchant plusieurs couples dans des ordres différents peuvent encore provoquer un interblocage : PostgreSQL annule alors une transaction, les contraintes empêchent toute divergence et l'API livrée renvoie 409. Une transaction externe longue peut aussi retarder une écriture ; les délais 5s/60s de V2 ne sont pas une limite générale de toutes les requêtes applicatives. Relire avant de réessayer ; aucune reprise automatique aveugle.

La suppression d'un enseignement ne supprime pas sa pondération. La suppression de classe/matière pondérée est refusée ; il faut retirer explicitement une pondération devenue inutilisée. L'historisation des coefficients et le gel/versionnement des bulletins restent hors lot : une prévisualisation recalculée utilise les coefficients courants. Les contraintes/triggers doivent rester actifs pour les écritures d'exploitation.

### Préservation et nettoyage

Contrôle final sur l'original, exclusivement en lecture seule : **16 empreintes avant/après identiques**, schéma identique à celui extrait de l'archive, séquences identiques à celles restaurées, Flyway V1 seule, PONDERER absente. Le conteneur original conserve l'identifiant `05233643cc525b0dc1bbcb13895709a8c67fb4eb153e7c6961f6ba8a640e1607` et son volume `gestion-eleves_postgres_data`.

La sauvegarde et les preuves de contrôle restent dans le répertoire privé indiqué plus haut. Les processus backend/frontend, conteneurs, volumes et fichiers temporaires de test ont été arrêtés/supprimés en ciblant uniquement les identifiants créés pour cette validation. Le rapport Playwright de fixtures reste consultable dans `frontend/playwright-report/` (ignoré par Git). Aucune migration sur l'original, aucune modification de ses données, aucun commit ni push.

## Procédure reproductible — migration de l'original non exécutée

La sauvegarde, la restauration et V2 sur copie ont été exécutées pendant la validation finale décrite ci-dessous. Les commandes suivantes restent une procédure à relire avant toute nouvelle exécution. L'étape 4 de migration réelle n'a pas été exécutée. Aucun backend ne doit démarrer sur l'existante pendant la validation. Prérequis : Docker, client CLI Flyway **11.7.2** avec support PostgreSQL (même version que le backend), dépôt à la branche livrée. Les commandes Compose utilisent uniquement psql/pg_dump de PostgreSQL 17 pour les lectures/sauvegardes.

### 1. Suspendre les écritures et vérifier V1 en lecture seule

Arrêter les instances backend et les autres écrivains pédagogiques selon leur mode de lancement. La suspension doit couvrir les empreintes avant/après. Depuis la racine du dépôt :

```bash
set -euo pipefail
sha256sum backend/src/main/resources/db/migration/V1__create_initial_schema.sql
mkdir -p /tmp/gestion-eleves-v2-validation

docker compose exec -T \
  -e PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=15000 -c lock_timeout=3000' \
  postgres psql -X -v ON_ERROR_STOP=1 -P pager=off \
  -U gestion_eleves -d gestion_eleves \
  < docs/merise/ponderer_preflight.sql

docker compose exec -T \
  -e PGOPTIONS='-c default_transaction_read_only=on' \
  postgres psql -X -qAt -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves \
  < docs/merise/ponderer_preservation.sql \
  > /tmp/gestion-eleves-v2-validation/existante-avant.txt
```

Exiger V1 réussie avec checksum `-526686782`, aucun coefficient contradictoire/invalide et le nombre courant de couples. **3 n'est qu'une attente de l'audit**, pas une précondition universelle. Le preflight s'arrête explicitement en cas d'anomalie. Résoudre un conflit par une décision métier séparée ; ne pas modifier V2 pour choisir une valeur.

### 2. Sauvegarder et restaurer sur un PostgreSQL distinct

Choisir un répertoire de sauvegarde protégé et durable. L'exemple suivant crée une archive avec un nom horodaté sous le répertoire de validation ; la déplacer aussi vers le stockage de sauvegarde prévu. Il n'effectue aucune migration de l'existante.

```bash
umask 077
validation_dump="/tmp/gestion-eleves-v2-validation/v1-$(date +%Y%m%dT%H%M%S).dump"
docker compose exec -T postgres pg_dump -U gestion_eleves -d gestion_eleves -Fc > "$validation_dump"
test -s "$validation_dump"
validation_container="gestion-eleves-v2-validation-$(date +%Y%m%dT%H%M%S)"
docker run --detach --rm --name "$validation_container" \
  --tmpfs /var/lib/postgresql/data:rw --publish 127.0.0.1:55440:5432 \
  --env POSTGRES_DB=gestion_eleves --env POSTGRES_USER=gestion_eleves \
  --env POSTGRES_PASSWORD=validation_only postgres:17
# Attendre que cette commande réussisse avant la restauration :
docker exec "$validation_container" pg_isready -U gestion_eleves -d gestion_eleves

docker exec -i "$validation_container" pg_restore --exit-on-error \
  -U gestion_eleves -d gestion_eleves < "$validation_dump"
docker exec -i "$validation_container" psql -X -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves < docs/merise/ponderer_preflight.sql
```

Si le port 55440 est déjà occupé, choisir un autre port libre et adapter toutes les URL de copie. Aucun volume de Compose ne doit être monté. Une archive PostgreSQL ne couvre pas les photos/documents externes : conserver aussi ces fichiers selon la configuration de stockage existante. Les données restaurées sont personnelles : protéger les accès et supprimer la copie après validation.

### 3. Appliquer et contrôler V2 sur la copie uniquement

```bash
export FLYWAY_URL='jdbc:postgresql://127.0.0.1:55440/gestion_eleves'
export FLYWAY_USER=gestion_eleves
export FLYWAY_PASSWORD=validation_only
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 info
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 -ignoreMigrationPatterns='*:pending' validate

docker exec -i "$validation_container" psql -X -qAt -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves < docs/merise/ponderer_preservation.sql \
  > /tmp/gestion-eleves-v2-validation/copie-avant.txt
diff -u /tmp/gestion-eleves-v2-validation/existante-avant.txt /tmp/gestion-eleves-v2-validation/copie-avant.txt

flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 migrate
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 validate

docker exec -i "$validation_container" psql -X -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves < docs/merise/ponderer_postflight.sql

docker exec -i "$validation_container" psql -X -qAt -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves < docs/merise/ponderer_preservation.sql \
  > /tmp/gestion-eleves-v2-validation/copie-apres.txt
diff -u /tmp/gestion-eleves-v2-validation/copie-avant.txt /tmp/gestion-eleves-v2-validation/copie-apres.txt
unset FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD
```

Exiger zéro différence dans les 16 tables d'origine, V2 réussie, couverture et égalité complètes, six contraintes attendues validées, deux triggers actifs, même nombre de pondérations que de couples source. Le postflight contrôle cette **sortie immédiate de migration** ; des pondérations sans enseignement ajoutées volontairement plus tard ne doivent pas être traitées comme une anomalie de reprise.

Pour vérifier l'application sur cette copie, ouvrir un terminal dédié depuis la racine du dépôt et exécuter uniquement cette configuration explicite :

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55440/gestion_eleves \
SPRING_DATASOURCE_USERNAME=gestion_eleves SPRING_DATASOURCE_PASSWORD=validation_only \
SPRING_FLYWAY_URL=jdbc:postgresql://127.0.0.1:55440/gestion_eleves \
SPRING_FLYWAY_USER=gestion_eleves SPRING_FLYWAY_PASSWORD=validation_only \
APPLICATION_BOOTSTRAP_ADMIN_EMAIL='' APPLICATION_BOOTSTRAP_ADMIN_PASSWORD='' \
SERVER_PORT=18081 ./backend/mvnw -f backend/pom.xml spring-boot:run
```

Vérifier calcul/PDF, CRUD ADMIN et refus hors ADMIN sur `http://127.0.0.1:18081`, puis arrêter ce processus avec Ctrl-C avant de supprimer sa copie. Ne pas lancer les E2E existants sans contrôler leur URL cible ; ils écrivent des données. L'ensemble des tests de cette livraison s'exécute séparément par `./scripts/test-ponderer.sh`, sur ses propres fixtures.

Après validation de la copie : conserver l'archive et les sorties de contrôle, puis supprimer seulement le conteneur temporaire :

```bash
docker rm -f "$validation_container"
```

### 4. Déploiement réel ultérieur, après revue

Ce lot n'exécute pas cette étape. Après revue et validation de la copie, conserver la suspension des écrivains et refaire l'étape 1 ainsi qu'une sauvegarde si les données ont changé. Pour l'instance Compose décrite dans ce dépôt, les commandes de migration réelle seraient les suivantes ; le mot de passe est saisi sans affichage :

```bash
export FLYWAY_URL='jdbc:postgresql://127.0.0.1:5432/gestion_eleves'
export FLYWAY_USER=gestion_eleves
read -r -s -p 'Mot de passe PostgreSQL : ' FLYWAY_PASSWORD
export FLYWAY_PASSWORD
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 info
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 -ignoreMigrationPatterns='*:pending' validate
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 migrate
flyway -locations="filesystem:$(pwd)/backend/src/main/resources/db/migration" -target=2 validate
unset FLYWAY_URL FLYWAY_USER FLYWAY_PASSWORD

docker compose exec -T -e PGOPTIONS='-c default_transaction_read_only=on' \
  postgres psql -X -v ON_ERROR_STOP=1 -U gestion_eleves -d gestion_eleves \
  < docs/merise/ponderer_postflight.sql

docker compose exec -T -e PGOPTIONS='-c default_transaction_read_only=on' \
  postgres psql -X -qAt -v ON_ERROR_STOP=1 -U gestion_eleves -d gestion_eleves \
  < docs/merise/ponderer_preservation.sql \
  > /tmp/gestion-eleves-v2-validation/existante-apres.txt
diff -u /tmp/gestion-eleves-v2-validation/existante-avant.txt /tmp/gestion-eleves-v2-validation/existante-apres.txt
```

Déployer le backend/frontend de cette branche seulement après succès. Ne pas exécuter V2 manuellement avec psql : Flyway doit enregistrer son checksum. Ne pas utiliser `repair`, `clean`, suppression de tables ou modification de V1 pour contourner un échec.

Si V2 échoue : sa transaction est annulée, laisser les écritures suspendues et analyser le message. Si elle réussit : conserver PONDERER et ses garanties ; un retour à une ancienne application garde les lectures et les écritures cohérentes, mais ses modifications locales de coefficient sont refusées. Aucun script destructif de retour arrière n'est livré.

## Points encore ouverts

Aucun blocage identifié pour ce lot après la restauration des données réelles, les contrôles Spring Boot et les 12 scénarios Playwright réussis. Le workflow CI a été relu et son YAML validé ; les mêmes scripts ont été exécutés localement, mais aucun job GitHub distant n'a été déclenché (aucun push). La politique de modification des coefficients en cours d'année et leur historique restent un futur arbitrage : les prévisualisations/PDF recalculés utilisent le coefficient courant. Versionnement, gel et validation des bulletins, héritage partiel/recouvrant et autres migrations de l'audit restent hors lot.

Les garde-fous SQL supposent des écritures ordinaires avec contraintes/triggers actifs. Les rôles d'exploitation ne doivent pas désactiver ces protections. La concurrence peut faire annuler une transaction par PostgreSQL ; il faut alors relire avant de réessayer. Aucun retry aveugle n'est ajouté.

## SQL exact de V2

Le fichier exécutable de référence est [V2__create_ponderer.sql](../../backend/src/main/resources/db/migration/V2__create_ponderer.sql).

```sql
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

```

## Fichiers ajoutés ou modifiés

**57 fichiers** dans le diff final : les 44 fichiers initiaux et 13 fichiers de documentation/infrastructure/recette supplémentaires.

- `.github/workflows/ci.yml`
- `.gitignore`
- `README.md`
- `backend/src/main/java/fr/afpa/backend/config/SecurityConfig.java`
- `backend/src/main/java/fr/afpa/backend/dto/bulletin/LigneBulletinDto.java`
- `backend/src/main/java/fr/afpa/backend/dto/enseignement/EnseignementRequest.java`
- `backend/src/main/java/fr/afpa/backend/dto/enseignement/EnseignementResponse.java`
- `backend/src/main/java/fr/afpa/backend/entity/Enseignement.java`
- `backend/src/main/java/fr/afpa/backend/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/fr/afpa/backend/repository/EnseignementRepository.java`
- `backend/src/main/java/fr/afpa/backend/service/BulletinCalculService.java`
- `backend/src/main/java/fr/afpa/backend/service/ClasseService.java`
- `backend/src/main/java/fr/afpa/backend/service/EnseignementService.java`
- `backend/src/main/java/fr/afpa/backend/service/MatiereService.java`
- `backend/src/test/java/fr/afpa/backend/BackendApplicationTests.java`
- `backend/src/test/java/fr/afpa/backend/security/AuthorizationHttpTest.java`
- `backend/src/test/java/fr/afpa/backend/security/SecurityIntegrationTest.java`
- `backend/src/test/java/fr/afpa/backend/service/BulletinCalculServiceTest.java`
- `backend/src/test/java/fr/afpa/backend/service/ClasseServiceTest.java`
- `backend/src/test/java/fr/afpa/backend/service/EnseignementServiceTest.java`
- `backend/src/test/java/fr/afpa/backend/service/MatiereServiceTest.java`
- `frontend/e2e/admin-bulletins.spec.js`
- `frontend/e2e/admin-enseignements.spec.js`
- `frontend/e2e/admin-evaluations.spec.js`
- `frontend/e2e/admin-notes.spec.js`
- `frontend/e2e/admin-scolarites.spec.js`
- `frontend/e2e/roles-access.spec.js`
- `frontend/eslint.config.js`
- `frontend/playwright.config.js`
- `frontend/src/App.jsx`
- `frontend/src/api/schoolApi.js`
- `frontend/src/components/school/ResourcePage.jsx`
- `frontend/src/pages/AdministrationPages.jsx`
- `frontend/src/pages/BulletinsPage.jsx`
- `frontend/src/pages/DashboardPage.jsx`
- `frontend/vite.config.js`
- `scripts/test-e2e.sh`
- `backend/src/main/java/fr/afpa/backend/controller/PondererController.java`
- `backend/src/main/java/fr/afpa/backend/dto/ponderer/PondererRequest.java`
- `backend/src/main/java/fr/afpa/backend/dto/ponderer/PondererResponse.java`
- `backend/src/main/java/fr/afpa/backend/entity/Ponderer.java`
- `backend/src/main/java/fr/afpa/backend/entity/PondererId.java`
- `backend/src/main/java/fr/afpa/backend/mapper/PondererMapper.java`
- `backend/src/main/java/fr/afpa/backend/repository/PondererRepository.java`
- `backend/src/main/java/fr/afpa/backend/service/PondererService.java`
- `backend/src/main/resources/db/migration/V2__create_ponderer.sql`
- `backend/src/test/java/fr/afpa/backend/integration/PondererIntegrationTest.java`
- `backend/src/test/java/fr/afpa/backend/migration/PondererMigrationTest.java`
- `backend/src/test/java/fr/afpa/backend/service/PondererServiceTest.java`
- `backend/src/test/java/fr/afpa/backend/support/IsolatedPostgres.java`
- `backend/src/test/resources/application.yaml`
- `docs/merise/LOT1_PONDERER.md`
- `docs/merise/ponderer_postflight.sql`
- `docs/merise/ponderer_preflight.sql`
- `docs/merise/ponderer_preservation.sql`
- `frontend/src/test/ponderations.test.jsx`
- `scripts/test-ponderer.sh`
