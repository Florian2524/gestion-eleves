# Lot 3 — Audit des périodes scolaires et de la cohérence temporelle

Audit du **28 octobre 2026**, dépôt `Florian2524/gestion-eleves`, branche `main`, commit de référence et HEAD observé : **`353f9fdc1466556ee07361d1204ab09d0eaa5402`**.

**Diagnostic : l’année scolaire de PERIODE manque effectivement. Son ajout nécessite une future migration SQL additive. Les incohérences potentielles viennent surtout de règles transversales absentes des services, des modifications possibles des parents et de décisions métier non arrêtées. Les trois périodes historiques se chevauchent toujours ; aucune donnée ne doit être corrigée automatiquement.**

Ce document constitue exclusivement un audit. Aucun code Java/React, configuration, migration, rôle SQL ou donnée métier n’a été modifié. Aucun démarrage Spring Boot, build, test, commit, push ni changement de branche n’est effectué pour cet audit. Les validations antérieures du commit de référence restent des preuves antérieures, pas des tests rejoués ici.

## 1. Diagnostic du modèle et du fonctionnement actuels

### Sources et méthode

Sources examinées : [AUDIT_MIGRATION.md](AUDIT_MIGRATION.md), [MCD.png](MCD.png), [MLD.png](MLD.png), [gestion-eleves.loo](gestion-eleves.loo), [MPD historique](mpd-postgresql.sql), migrations V1/V2, catalogue et données PostgreSQL actuels, entités, services, repositories, DTO/mappers, contrôleurs/sécurité, écrans et tests existants. Les deux PNG ont été inspectés visuellement. Le fichier Looping est binaire ; aucune extraction fiable de ses cardinalités n’est revendiquée. Le MPD historique reste identique à V1 ; il ne décrit donc pas à lui seul l’état V2.

Les observations PostgreSQL principales ont été relevées le **28/10/2026 à 10:25:22 UTC**, sur **PostgreSQL 17.10**. Les sessions `psql -X`, avec arrêt sur erreur et délais de requête/verrou, imposent `default_transaction_read_only=on`, puis une transaction **REPEATABLE READ READ ONLY** ; `transaction_read_only=on` est vérifié. Chaque session se termine par `ROLLBACK`. Il n’y a ni `nextval`, ni écriture, ni migration, ni modification de configuration SQL. Aucun autre client de cette base n’était connecté lors du relevé. Des agrégats complémentaires sont lus dans une seconde transaction protégée ; un manifeste final vérifie la conservation de l’ensemble.

Les manifestes privés comparent les empreintes SHA-256 des lignes et des PK des **18 tables**, l’état des **12 séquences** (`last_value`, `is_called`, sans les avancer), les métadonnées et Flyway. Le manifeste initial est identique à celui établi après la fusion du correctif photo. Aucune donnée personnelle, secret ou hash de mot de passe n’est reproduit ici.

### Relations et responsabilités

| Objet | Modèle métier / relation actuelle | Garanties et limites actuelles |
|---|---|---|
| PERIODE | Un calendrier identifié ; une évaluation et un bulletin désignent chacun exactement une période. Plusieurs évaluations/bulletins peuvent la référencer. | Dates obligatoires, fenêtre facultative, statut texte. Aucune année ni relation directe à une classe. |
| SCOLARITE | Un élève dans une classe sur un intervalle ; les notes et les bulletins physiques actuels y sont rattachés. | Début obligatoire, fin nullable ; doublon exact élève/classe/début empêché. Le recouvrement n’est pas interdit. |
| INSCRIPTION | Inscription administrative d’un élève, indépendante des affectations à une classe. | Pas de FK vers SCOLARITE ni PERIODE dans le MLD ou la base. Couverture temporelle non vérifiée. |
| ENSEIGNEMENT | Enseignant, classe et matière, dates, indicateur actif. L’année vient de CLASSE. | Doublon exact empêché ; pas de cohérence temporelle avec ses évaluations. Le coefficient de référence est désormais PONDERER. |
| EVALUATION | Un enseignement et une période, date civile, barème et coefficient propres. | FK et positivité des valeurs ; aucune garantie date dans période/enseignement ou année compatible. |
| NOTE | Une scolarité et une évaluation ; valeur, statut, instant de création. | Unicité du couple ; même classe et valeur ≤ barème vérifiées par le service, mais dates/fenêtre/statut de période ignorés. |
| BULLETIN | Le MLD cible l’élève et une période ; le schéma actuel conserve une scolarité et une période. | Le parcours disponible est une prévisualisation calculée et un PDF recalculé, sans sauvegarde. Le versionnement est un autre lot. |

### Ce que font réellement les services

- `PeriodeService` assure un CRUD simple. La lecture est transactionnelle en lecture seule, les mutations en écriture. Il ne consulte pas les références avant modification/suppression ; aucune recherche par année, détection de recouvrement, fermeture métier ou sérialisation temporelle n’existe.
- `PeriodeRequest` vérifie libellé/statut non vides, tailles et dates début ≤ fin, y compris pour une fenêtre dont les deux bornes sont présentes. Une fenêtre à une seule borne et une fenêtre extérieure à la période sont acceptées. Le statut reste une chaîne libre de 30 caractères.
- `EvaluationService` charge enseignement/période ; il protège la suppression d’une évaluation avec notes. Sa modification peut changer date, période, enseignement ou barème sans revalider les notes existantes.
- `NoteService` vérifie doublon, classe de la scolarité = classe de l’enseignement, valeur ≤ barème ; le DTO impose une valeur positive ou nulle. Création/modification/suppression ne tiennent pas compte des dates de scolarité, fenêtres, fermeture ou exceptions. `Note.dateSaisie` est un `OffsetDateTime` avec `@CreationTimestamp`, non modifiable par JPA : **ce n’est pas une date de dernière modification**.
- `ScolariteService` protège seulement le doublon exact. Élève, classe et dates restent modifiables malgré des notes/bulletins ; la suppression repose sur les FK. `InscriptionService` ne rapproche pas les inscriptions des scolarités. Les DTO vérifient l’ordre de leurs propres dates, pas les relations.
- `EnseignementService` préserve les protections de suppression en présence d’évaluations/lignes de bulletin et la compatibilité PONDERER. Une modification de classe, matière ou dates ne revalide pas les dépendances. `ClasseService.update` peut changer l’année d’une classe déjà utilisée. Les verrous de PERSONNE du lot 2 ne constituent pas un protocole de cohérence du calendrier.

### Calculs et PDF : acquis à conserver

`BulletinCalculService.calculer(idScolarite,idPeriode)` sélectionne les enseignements de la classe dont les dates intersectent la période, avec bornes inclusives et fin d’enseignement NULL assimilée à une absence de borne. Les notes sont recherchées par **identifiants de scolarité et de période**, sans filtre supplémentaire sur leur date d’évaluation, statut, fenêtre de saisie ou année scolaire. Une évaluation mal datée peut donc rester comptée si son enseignement intersecte la période ; un enseignement déplacé hors période peut faire disparaître ses résultats du calcul.

Le lot 1 regroupe les notes par matière, applique une seule pondération classe–matière de PONDERER, normalise les barèmes sur 20 et conserve les coefficients d’évaluation, précision de calcul 10, affichage 2 décimales `HALF_UP`. Les matières sans moyenne sont exclues du dénominateur général ; une pondération absente/invalide déclenche une erreur explicite. Ne pas modifier ces règles en ajoutant l’année.

Il n’existe pas de contrôle de compatibilité période/classe/scolarité. `BulletinDto.anneeScolaire` provient actuellement de **la classe**, et non de la période. Les GET `/bulletins/calcul/{idScolarite}/{idPeriode}` et `/pdf` restent sans écriture ; `BulletinPdfService` recalcule le résultat à chaque téléchargement. Modifier les dates, rattachements ou coefficients peut donc modifier le PDF futur. Ce comportement n’est pas celui d’un bulletin officiel immuable.

## 2. Écarts avec le MLD et périmètre du lot

| Écart | Situation à ce commit | Traitement recommandé |
|---|---|---|
| `PERIODE.annee_scolaire` | Présent au MCD/MLD, absent en SQL, JPA, DTO et React. | Cœur du lot 3 : ajout additif puis reprise contrôlée et validations. |
| `EVALUATION.motif_exception`, `NOTE.motif_exception` | Présents au MLD, absents de la base et des contrats actuels. | À inclure seulement si le lot 3 doit réellement ouvrir des dérogations, après définition de leurs cas et droits. Aucune raison historique à inventer. |
| Règles date/année entre relations | Les FK prouvent l’existence, pas la cohérence temporelle. | Services atomiques sur toutes les mutations concernées ; contrôles SQL locaux adaptés. |
| Champs de versions, instantanés et publication BULLETIN/LIGNE_BULLETIN | Toujours absents ; tables physiques historiques conservées. | Hors implémentation du lot 3. Décrire les conséquences sans anticiper ce chantier. |
| `habilitation_validation` du compte | Absente ; les rôles actuels restent ADMIN/ENSEIGNANT/RESPONSABLE. | Ne pas introduire cette capacité ou un système de rôles pour gérer simplement une période. |
| Ancien coefficient d’ENSEIGNEMENT | Conservé en transition V2 ; PONDERER fait autorité et reste synchronisé. | Aucun retrait ni changement dans ce lot. |

Le MCD/MLD n’impose ni une interdiction de chevauchement, ni un calendrier septembre–août, ni l’unicité d’un libellé, ni la signification exacte des statuts/fenêtres. Il ne faut pas transformer ces hypothèses en contraintes. Les anciens numéros V3…V13 proposés dans `AUDIT_MIGRATION.md` sont une feuille de route historique, pas des migrations existantes ni une réservation définitive des numéros.

## 3. Règles temporelles manquantes et niveaux de contrôle

### Répartition base / DTO / services

| Sujet | Déjà garanti | Vérification à prévoir, sous réserve des décisions de la section 5 |
|---|---|---|
| Dates d’une période | SQL `ck_periode_dates` et DTO : fin ≥ début, deux dates obligatoires. | Conserver les périodes d’un seul jour si aucune règle différente n’est décidée ; dates civiles finies, pas de durée maximale arbitraire. |
| Fenêtre de saisie | SQL `ck_periode_dates_saisie` et DTO : ordre si les deux dates existent. | Définir les quatre cas de nullabilité ; appliquer la fenêtre lors de chaque mutation de note, et éventuellement d’évaluation, pas seulement lors de la création de PERIODE. |
| Fenêtre dans/hors période | Rien. | Une saisie après la fin pédagogique peut être légitime. Ne pas imposer son inclusion avant arbitrage. |
| Statut et fermeture | Texte non vide via DTO ; pas de CHECK SQL de vocabulaire. | Définir transitions et matrice d’opérations : créer/modifier/supprimer notes et évaluations, rouvrir, consulter/calculer/exporter. Ne pas bloquer la lecture historique parce qu’une période est fermée. |
| Année scolaire | CLASSE a une chaîne obligatoire, seulement `@NotBlank`/taille 20. | Même convention pour CLASSE et PERIODE ; année de période compatible avec la classe de chaque enseignement et avec chaque scolarité de bulletin. |
| Date d’évaluation | `DATE NOT NULL`, FK présentes. | Date dans la période **et** l’enseignement ; comparer année de période et classe ; décider les évaluations futures et exceptions. |
| Note et scolarité | Même classe vérifiée dans `NoteService`. | La date de l’évaluation doit être dans la scolarité ; vérifier l’éligibilité d’une inscription si cette règle est décidée. Ne pas confondre date d’évaluation et instant de saisie. |
| Inscription | Fin NULL ou ≥ date d’inscription, SQL/DTO. | Définir si une inscription couvre toute la scolarité ou seulement les dates pédagogiques ; couverture par une inscription ou une union de plusieurs ; statuts admis. Aucune FK artificielle déduite. |
| Scolarité | Ordre des dates et doublon exact. | Simultanéité, changement de classe, année de classe et protection des notes/bulletins lors d’un changement d’élève/classe/dates. Une fin NULL n’est pas une preuve d’activité éternelle. |
| Enseignement | Ordre des dates, doublon exact, suppression protégée. | Dates et année compatibles ; changement de classe/matière/enseignant/dates revalidé face aux évaluations et notes. Coenseignement et remplacement peuvent expliquer un recouvrement. |
| Modification d’une période utilisée | Aucun contrôle métier. | Recommandation : interdire changement d’année/dates invalidant des références ; choisir gel strict des champs structurants ou revalidation complète. Encadrer aussi libellé/fenêtre/statut, car ils affectent les lectures/PDF et les saisies futures. |
| Suppression d’une période | FK depuis EVALUATION et BULLETIN, `NO ACTION`. | Vérifier explicitement les deux usages et retourner 409 métier. Ne jamais supprimer notes/évaluations/bulletins en cascade pour contourner le refus. Une évaluation sans note suffit déjà à bloquer. |
| Chevauchement | Autorisé de fait ; aucune exclusion/unicité temporelle. | Définir portée : toutes périodes, même année, même type/calendrier/groupe ? Les structures de type/groupe ne sont pas présentes. Aucune contrainte nouvelle avant décision. |
| Bulletin calculé | Existence des deux objets ; filtre d’intersection d’enseignements. | Refuser une combinaison d’années incompatibles ; décider le traitement d’une scolarité sans intersection. Ne pas masquer les anomalies en supprimant silencieusement des notes du résultat. |

Les CHECK SQL conviennent à l’ordre des dates, au format et aux valeurs d’une même ligne. Les vérifications entre tables ne doivent pas être simulées par un CHECK appelant une fonction lisant d’autres tables. Si d’autres écrivains SQL existent, une garantie au-delà des services demandera une conception spécifique (triggers ciblés ou clés adaptées), pas une promesse de la seule FK. [PostgreSQL 17 — CHECK](https://www.postgresql.org/docs/17/ddl-constraints.html#DDL-CONSTRAINTS-CHECK-CONSTRAINTS).

### Instant de saisie, fuseau et historique

Les intervalles pédagogiques sont des `DATE` / `LocalDate`. L’instant de saisie est un `TIMESTAMPTZ` / `OffsetDateTime`. Pour contrôler une fenêtre exprimée en jours, choisir un fuseau métier explicite et un `Clock` injectable ; `Europe/Paris` est une proposition à confirmer, pas une déduction de la timezone SQL UTC. Vérifier l’instant réel de l’opération, y compris une modification ou suppression, plutôt que l’ancienne `dateSaisie` immuable. Tester minuit et les changements d’heure ; le navigateur ne doit pas décider seul si une action est permise.

Ne pas ajouter un CHECK fondé sur « aujourd’hui » : une ligne historique ne doit pas devenir invalide avec le temps. Les indicateurs `actif`, `ACTIVE`, `VALIDEE`, `OUVERTE` n’ont pas de définition temporelle complète. Un enseignement devenu inactif ne doit pas être retiré d’un bulletin historique sans décision explicite.

### Mutations des parents et concurrence

Contrôler uniquement la création de NOTE/EVALUATION ne suffit pas : il faut également couvrir les changements de PERIODE, CLASSE, ENSEIGNEMENT, SCOLARITE et, si la couverture devient obligatoire, INSCRIPTION. Exemple : une note initialement valide peut dépasser le barème après modification de l’évaluation, ou concerner une autre classe après déplacement de l’enseignement.

Pour les périodes, étudier un verrou pessimiste de la ligne PERIODE partagé par création/modification d’évaluation, mutations de notes et modification/fermeture/suppression de la période. Relire les données après acquisition du verrou ; ne pas utiliser une entité déjà chargée devenue obsolète. Lors d’un déplacement entre deux périodes, verrouiller les deux dans un ordre déterministe. Les autres objets parents nécessitent le même examen. Définir un ordre global compatible avec les verrous PERSONNE/PONDERER existants et vérifier les interblocages en tests. Une vérification `exists…` puis `save` sans protocole commun présente une fenêtre de course. Un `@Version` sur PERIODE seul ne protège pas toutes les écritures de ses enfants et demanderait une colonne technique supplémentaire, non exigée par le MLD. [PostgreSQL 17 — verrous et interblocages](https://www.postgresql.org/docs/17/explicit-locking.html).

Si le non-recouvrement est finalement exigé, verrouiller les seules périodes existantes ne protège pas l’espace vide entre deux créations concurrentes. Il faudra un mécanisme couvrant ce cas, puis éventuellement une exclusion SQL après validation du périmètre et résolution approuvée des données. Le simple filtre applicatif par année n’est pas une garantie concurrente.

## 4. Inventaire des données PostgreSQL actuelles

### Périodes et références

| id_periode | Début | Fin | Statut | Début/fin saisie | Évaluations | Notes indirectes | Bulletins | Années issues des références |
|---:|---|---|---|---|---:|---:|---:|---|
| 17 | 2026-09-01 | 2026-12-31 | OUVERTE | NULL / NULL | 1 | 1 | 0 | 2026-2027 |
| 20 | 2026-09-01 | 2026-12-31 | OUVERTE | NULL / NULL | 1 | 1 | 0 | 2026-2027 |
| 28 | 2026-09-01 | 2026-12-31 | OUVERTE | NULL / NULL | 1 | 1 | 0 | 2026-2027 |

Les libellés sont distincts. Les **trois paires** `(17,20)`, `(17,28)`, `(20,28)` se recouvrent intégralement avec bornes inclusives. Les évaluations datent toutes du **20/10/2026**. Chaque période a une année source unique via `evaluation → enseignement → classe`. La voie `bulletin → scolarite → classe` a aussi été examinée : aucun bulletin n’existe actuellement.

Les seules FK directes entrantes vers PERIODE sont `fk_evaluation_periode` et `fk_bulletin_periode`, toutes deux validées, non différables, sans suppression en cascade. NOTE dépend indirectement de PERIODE par EVALUATION ; LIGNE_BULLETIN par BULLETIN. INSCRIPTION, SCOLARITE et ENSEIGNEMENT n’ont pas de FK directe vers PERIODE.

**Implications :** une exclusion générale, y compris limitée à l’année candidate `2026-2027`, échouerait sur ces données. Ne pas fusionner les trois périodes, réaffecter leurs évaluations, renommer ou réduire leurs dates. Leurs identifiants distincts partitionnent actuellement les notes et les calculs ; des dates identiques ne rendent pas les ressources interchangeables.

### Autres données et contrôles

| Ensemble | État observé |
|---|---|
| CLASSE | 4 lignes, toutes en `2026-2027` ; format `AAAA-AAAA` et années consécutives sur cet état. |
| SCOLARITE | 3 lignes, statut ACTIVE, début 2026-09-01, fin NULL. |
| INSCRIPTION | 0 ligne : les **3 scolarités n’ont pas d’inscription couvrante**. |
| ENSEIGNEMENT | 3 lignes, début 2026-09-01, fin NULL, tous actifs. |
| EVALUATION / NOTE | 3 / 3 lignes ; notes : 2 SAISIE, 1 VALIDEE. |
| BULLETIN / LIGNE_BULLETIN | 0 / 0 ligne ; aucune conversion d’historique à effectuer à cet instant, à revérifier au déploiement. |
| PONDERER | 3 couples ; aucune divergence avec le coefficient hérité conservé dans ENSEIGNEMENT. |
| Identités et comptes | 8 personnes, 4 élèves, 3 enseignants, 0 responsable ; 1 compte ADMIN actif préservé. |
| Autres tables | 3 matières, 0 responsabilité légale ; les 18 tables incluent les 2 lignes Flyway. |

| Contrôle exécuté | Résultat |
|---|---:|
| Couples de périodes qui se recouvrent | **3** |
| Périodes avec dates inversées / fenêtres inversées / une seule borne | 0 / 0 / 0 |
| Fenêtres renseignées hors de la période | 0, **aucune fenêtre n’est renseignée** |
| Évaluations hors période / hors enseignement | 0 / 0 |
| Notes avec date d’évaluation hors scolarité / classe incompatible / hors barème | 0 / 0 / 0 |
| Notes hors fenêtre renseignée | 0, contrôle sans portée pour les fenêtres NULL présentes |
| Recouvrements de scolarités d’un même élève | 0 |
| Recouvrements d’enseignements d’un même couple classe–matière | 0 |
| Scolarités sans inscription couvrante | **3** |
| Années de CLASSE non canoniques / non consécutives | 0 / 0 |
| Dates civiles non finies sur les colonnes temporelles contrôlées | 0 |
| Notes dont la saisie précède la date d’évaluation | **3** |

Les instants de création des notes s’étendent du **03/10/2026 au 04/10/2026 UTC**, avant les évaluations du 20/10. Cette pré-saisie existe ; le modèle ne prouve pas qu’elle est interdite. Ne pas imposer une règle rétroactive `date_saisie ≥ date_evaluation` ni réécrire ces instants sans décision. Ne pas attribuer l’origine de ces lignes à un utilisateur ou à un test sur la seule apparence des libellés.

Flyway contient exclusivement **V1 réussie** (checksum `-526686782`) et **V2 réussie** (`1440610697`). Les 12 séquences ont été lues sans avancement. La comparaison des données complètes, identifiants et métadonnées avec le contrôle de fin de fusion photo ne présente aucune différence.

### Reproductibilité des constats essentiels

À exécuter uniquement avec `psql -X`, `ON_ERROR_STOP=1`, `PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=30000 -c lock_timeout=5000'`. Ce bloc est une lecture d’audit, pas une migration ; aucun résultat personnel n’est demandé.

```sql
BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT current_setting('transaction_read_only'), current_timestamp;

SELECT p.id_periode, p.date_debut, p.date_fin, p.statut,
       p.date_debut_saisie, p.date_fin_saisie,
       (SELECT count(*) FROM evaluation e
        WHERE e.id_periode = p.id_periode) AS evaluations,
       (SELECT count(*) FROM bulletin b
        WHERE b.id_periode = p.id_periode) AS bulletins
FROM periode p ORDER BY p.id_periode;

SELECT a.id_periode, b.id_periode
FROM periode a JOIN periode b
  ON a.id_periode < b.id_periode
 AND a.date_debut <= b.date_fin AND b.date_debut <= a.date_fin
ORDER BY 1, 2;

WITH sources AS (
  SELECT e.id_periode, c.annee_scolaire
  FROM evaluation e JOIN enseignement en USING (id_enseignement)
  JOIN classe c USING (id_classe)
  UNION
  SELECT b.id_periode, c.annee_scolaire
  FROM bulletin b JOIN scolarite s USING (id_scolarite)
  JOIN classe c USING (id_classe)
)
SELECT p.id_periode,
       count(DISTINCT s.annee_scolaire) AS nombre_annees_sources,
       array_agg(DISTINCT s.annee_scolaire)
         FILTER (WHERE s.annee_scolaire IS NOT NULL) AS annees_sources
FROM periode p LEFT JOIN sources s USING (id_periode)
GROUP BY p.id_periode ORDER BY p.id_periode;
ROLLBACK;
```

Ces requêtes ne déduisent pas l’année des dates. Pour une campagne de migration, les compléter par la validation des valeurs sources, des éventuelles valeurs déjà renseignées et la comparaison des projections d’origine de toutes les tables.

## 5. Décisions métier à prendre avant les développements dépendants

| Décision | Questions à trancher | Proposition technique soumise à décision |
|---|---|---|
| D1 — Année et calendrier | Format `2026-2027` obligatoire ? Années consécutives ? Bornes du calendrier, fuseau et années extrêmes acceptées ? | Chaîne canonique commune CLASSE/PERIODE ; aucun calcul automatique à partir des mois. Le calendrier septembre–août reste à confirmer. |
| D2 — Portée et chevauchement | Calendrier global ou périodes parallèles par type/groupe ? Pourquoi les 3 périodes identiques ? | Conserver les trois ressources. Autoriser provisoirement les recouvrements tant que le métier n’a pas décidé autre chose ; aucune EXCLUDE/UNIQUE nouvelle. |
| D3 — Bornes | Dates inclusives ? Période d’un jour ? Fins de scolarité/enseignement NULL signifiant quelle durée ? | Garder les bornes inclusives actuelles ; ne pas forcer de fin historique par calcul implicite. |
| D4 — Fenêtres et statuts | NULL/NULL = libre ou non configuré ? Borne unique = fenêtre ouverte ou erreur ? Une fenêtre peut-elle déborder ? Quelles opérations sont interdites quand la période est fermée ? | Écrire une matrice par opération et état ; décider la réouverture et les contrôles sur création, modification et suppression. |
| D5 — Exceptions et droits | Exceptions admises pour date hors période, fermeture, fenêtre ? Qui les autorise ? Motif, trace et conservation ? | Ne créer aucune dérogation sur la seule présence d’un rôle ADMIN/ENSEIGNANT ou d’un texte libre ; séparer autorisation et justification. |
| D6 — Période déjà utilisée | Geler année/dates, ou permettre un changement compatible après revalidation ? Fenêtres nouvelles rétroactives ou seulement prospectives ? | Refuser tout changement invalidant des dépendances ; préférer le gel des champs structurants si aucune procédure de correction n’est définie. |
| D7 — Inscriptions | Couverture obligatoire, à quelle date et selon quel statut ? Application prospective ou aussi aux 3 scolarités historiques ? | Pas de fausse inscription créée pour satisfaire une nouvelle règle ; résolution explicite ou maintien historique documenté. |
| D8 — Mobilité et simultanéité | Plusieurs scolarités simultanées ? Remplacement/coenseignement ? Coefficient lors d’un transfert ? | Préserver les liens existants et le calcul par scolarité ; ne pas anticiper le bulletin multi-scolarités par élève. |
| D9 — Saisie anticipée et notes retenues | Pré-saisie avant évaluation autorisée ? Notes SAISIE et VALIDEE toutes retenues ? Effet des statuts inactifs sur l’historique ? | Conserver le calcul courant tant qu’une décision ne demande pas explicitement son changement ; tester les 3 pré-saisies existantes. |
| D10 — Reprise des années | Approuver `17 → 2026-2027`, `20 → 2026-2027`, `28 → 2026-2027` d’après les références ? Traitement des périodes sans source ou avec sources contradictoires ? | Validation explicite de cette correspondance ; arrêt sur ambiguïté, pas d’année courante ou de valeur déduite des dates. |
| D11 — Compatibilité de déploiement | Année requise dès le premier POST/PUT ? Anciens clients/processus simultanés ? | Phase nullable et code de transition, puis obligation coordonnée ; ne pas fournir un défaut serveur arbitraire. |

Les choix D1/D2/D4/D6/D10/D11 bloquent la définition complète du contrat et de sa migration. Les choix D5/D7/D8/D9 bloquent les règles dépendantes : on peut les sortir explicitement du premier incrément, mais pas les décider silencieusement. Aucun choix sur le versionnement/publication des bulletins n’est nécessaire pour ajouter une année de période ; ce chantier reste séparé.

## 6. Plan d’évolution Java/JPA et inventaire précis

Chemins relatifs au dépôt. Dans les tableaux suivants, un répertoire suivi de noms de fichiers désigne chacun des fichiers nommés, sans supposer qu’ils devront tous être modifiés. **Direct** = cœur de l’ajout ; **conditionnel** = selon les règles choisies ; **régression** = préserver et tester sans modification prévue.

| Fichiers | Niveau et adaptation |
|---|---|
| `backend/src/main/java/fr/afpa/backend/entity/Periode.java` | Direct : `String anneeScolaire`, colonne `annee_scolaire`, longueur 20, d’abord nullable ; constructeur et accès. Aucune nouvelle entité calendrier exigée. |
| `backend/src/main/java/fr/afpa/backend/dto/periode/PeriodeRequest.java`, `PeriodeResponse.java` | Direct : propriété JSON additive `anneeScolaire`, validation selon phase. Conserver noms/formats des dates et champs actuels. |
| `backend/src/main/java/fr/afpa/backend/mapper/PeriodeMapper.java` | Direct : lecture/création/modification du champ, sans défaut déduit. |
| `backend/src/main/java/fr/afpa/backend/repository/PeriodeRepository.java` | Direct : filtre exact d’année et ordre stable ; accès verrouillé si retenu. Pas d’interdiction de recouvrement implicite. |
| `backend/src/main/java/fr/afpa/backend/service/PeriodeService.java` | Direct : recherche, contrôles des références et mutations, messages métier, protocole de concurrence. |
| `backend/src/main/java/fr/afpa/backend/controller/PeriodeController.java` | Direct : GET filtrable optionnel ; JSON et codes HTTP existants conservés autant que possible. |
| `backend/src/main/java/fr/afpa/backend/service/EvaluationService.java`, `NoteService.java`, `ScolariteService.java`, `InscriptionService.java`, `EnseignementService.java`, `ClasseService.java` | Conditionnel mais nécessaire au périmètre temporel retenu : vérifier les relations et revalider les enfants lors d’une mutation du parent. Ne pas relâcher les contrôles PONDERER/profils. |
| `backend/src/main/java/fr/afpa/backend/repository/EvaluationRepository.java`, `BulletinRepository.java`, `NoteRepository.java`, `ScolariteRepository.java`, `InscriptionRepository.java`, `EnseignementRepository.java`, `ClasseRepository.java` | Requêtes d’existence par période, anomalies/dépendances, recherches d’intervalles et verrous selon protocole. Les méthodes de sécurité existantes restent valides. |
| `backend/src/main/java/fr/afpa/backend/dto/classe/ClasseRequest.java`, `dto/evaluation/EvaluationRequest.java`, `dto/note/NoteRequest.java`, `dto/scolarite/ScolariteRequest.java`, `dto/inscription/InscriptionRequest.java`, `dto/enseignement/EnseignementRequest.java` | Conditionnel : convention d’année commune et validations locales. Les contrôles nécessitant la base restent dans les services, pas dans des validateurs DTO accédant implicitement à SQL. |
| `backend/src/main/java/fr/afpa/backend/service/BulletinCalculService.java`, `BulletinPdfService.java`, `dto/bulletin/BulletinDto.java` | Compatibilité année/classe/période ; affichage explicite et erreurs utiles. Conserver l’origine actuelle du champ année du bulletin, ou ajouter un champ distinct si nécessaire ; pas de changement silencieux de sens. |
| `backend/src/main/java/fr/afpa/backend/entity/Evaluation.java`, `Note.java`, `mapper/EvaluationMapper.java`, `NoteMapper.java`, `dto/evaluation/EvaluationResponse.java`, `dto/note/NoteResponse.java` | Conditionnel : motifs d’exception si décidés. Les associations existantes suffisent aux contrôles ; aucune duplication obligatoire d’année dans NOTE/EVALUATION. |
| `backend/src/main/java/fr/afpa/backend/entity/Classe.java`, `Scolarite.java`, `Inscription.java`, `Enseignement.java`, `Bulletin.java`, `LigneBulletin.java` ; `mapper/ClasseMapper.java`, `ScolariteMapper.java`, `InscriptionMapper.java`, `EnseignementMapper.java` sous la même racine Java | Régression/inspection des mutations. Pas de changement structurel exigé par le seul ajout d’année à PERIODE. BULLETIN et LIGNE_BULLETIN ne sont pas versionnés dans ce lot. |
| `backend/src/main/java/fr/afpa/backend/controller/EvaluationController.java`, `NoteController.java`, `ScolariteController.java`, `InscriptionController.java`, `EnseignementController.java`, `ClasseController.java`, `BulletinController.java` | Compatibilité API/erreurs ; conserver les contrôles d’accès et les GET de calcul/PDF sans effet de bord. Modification uniquement si contrat réellement enrichi. |
| `backend/src/main/java/fr/afpa/backend/exception/GlobalExceptionHandler.java`, `BusinessRuleViolationException.java`, `ResourceInUseException.java` | Réutiliser les erreurs : 400 validation/règle, 404 absent, 409 référence/concurrence. Une période référencée doit produire un message métier précis, au lieu du seul 409 d’intégrité générique. |
| `backend/src/main/java/fr/afpa/backend/config/SecurityConfig.java`, `security/SecurityExpressions.java` ; `repository/ScolariteRepository.java`, `EvaluationRepository.java`, `EnseignementRepository.java` | Régression et examen de la portée historique. GET périodes pour les trois rôles ; mutations de périodes ADMIN seulement ; propriété des évaluations/notes conservée. |
| `backend/src/main/java/fr/afpa/backend/security/ActiveAccountJwtValidator.java`, `JwtService.java`, `service/CompteUtilisateurService.java`, `FirstAdminBootstrap.java` ; entités PERSONNE/profils et service photo | Régression uniquement : ni nouveau rôle, ni permission par cumul de profils, ni attribution de dérogation implicite ; bootstrap inchangé et neutralisé sur les bases de test. |

**Type et format recommandés :** SQL `VARCHAR(20)` pour rester aligné avec CLASSE, Java `String`, format canonique métier proposé **`AAAA-AAAA`** de 9 caractères (exemple `2026-2027`), seconde année = première + 1. Ne pas utiliser une `DATE`, une chaîne libre différente de CLASSE, ni une année courante calculée. Un validateur partagé de format et de succession, appelé par le DTO et le service, évite les divergences ; `@NotBlank` devient obligatoire seulement à la phase coordonnée. Les bornes d’année acceptées et la normalisation des espaces sont à décider, puis testées. Le MLD n’impose pas la taille SQL.

**Filtres recommandés :** `GET /periodes?anneeScolaire=2026-2027`, optionnel ; absence du filtre = liste complète, pas « année courante ». Conserver la réponse tableau et les GET par identifiant. Prévoir un ordre déterministe année/début/id, des recherches de libellé éventuelles sans en faire une clé, et un rejet 400 d’un filtre invalide. Ne pas ajouter d’index prématurément : trois lignes ne justifient pas à elles seules un index année ; mesurer aussi les recherches de références (`evaluation.id_periode`, `bulletin.id_periode`) non couvertes en première colonne par un index métier existant.

**Sécurité à préserver :** la consultation de PERIODE est globale pour les rôles autorisés ; la gestion du calendrier reste ADMIN. Les mutations d’évaluations/notes sont ADMIN ou ENSEIGNANT propriétaire ; RESPONSABLE est lecteur. La portée actuelle de certaines requêtes d’autorisation est fondée sur la classe/l’élève sans intersection de dates. Décider séparément les droits de consultation historique avant de les restreindre ou élargir. Un filtre d’année fourni par le client n’est jamais une autorisation. Le cumul enseignant/responsable ne crée aucun droit supplémentaire. Une fermeture éventuelle est une règle métier en plus de l’autorisation, pas un remplacement de celle-ci.

## 7. Plan React et fichiers concernés

| Fichier | Adaptation proposée |
|---|---|
| `frontend/src/pages/AdministrationPages.jsx` | PERIODE : champ et colonne année ; statut/fenêtre selon décisions ; messages lors de modification/suppression protégée. Évaluations : choix de périodes compatible avec la classe/année, sans sélection automatique d’une période par les seules dates. Classes/scolarités/enseignements/inscriptions : feedback des règles approuvées. |
| `frontend/src/components/school/schoolData.js` | Libellé explicite `libellé · année`, éventuellement dates pour les périodes homonymes ; conserver l’identifiant comme valeur. |
| `frontend/src/components/school/ResourcePage.jsx` | Support ciblé de filtres/recherches et champs dépendants si nécessaire ; préserver les parcours de rattachement PERSONNE et les profils multiples. Le composant actuel ne filtre pas les options par classe/année. |
| `frontend/src/api/schoolApi.js`, `api/bulletinsApi.js` | Ajouter les paramètres de recherche sans casser le passage `signal`/options ni les chemins actuels. `api/adminApi.js` gère les liens légaux, pas les périodes : pas de modification nécessaire. |
| `frontend/src/pages/BulletinsPage.jsx` | Choix d’année explicite et périodes compatibles avec la scolarité ; réinitialiser les sélections devenues invalides et le résultat/PDF affiché. L’écran actuel trie par début puis sélectionne la première période, indépendamment de la scolarité ; aucune garantie d’année. |
| `frontend/src/pages/NotesPage.jsx` | Afficher année et fenêtre/statut ; les scolarités proposées sont actuellement filtrées seulement par classe. Enrichir selon date d’évaluation et règles décidées ; conserver les refus serveur et la consultation historique. |
| `frontend/src/api/notesApi.js`, `api/http.js` | Inspection de compatibilité ; adaptation seulement si motifs ou contrats d’erreur changent. Aucun besoin de nouvelle permission ou nouveau stockage auth. |
| `frontend/src/App.jsx`, `pages/DashboardPage.jsx`, `components/auth/ProtectedRoute.jsx` | Régression navigation/permissions ; la route `/periodes` reste ADMIN, les listes de périodes restent accessibles dans les formulaires des autres rôles autorisés. Aucune nouvelle route obligatoire. |

Les chaînes `AAAA-AAAA` ne doivent pas être converties en nombre dans `ResourcePage`. Si le champ devient un select de chaînes, distinguer explicitement ce cas des selects de FK que le composant convertit aujourd’hui en `Number`. Les dates restent `YYYY-MM-DD` ; éviter les conversions UTC qui décalent le jour. Représenter une année NULL de transition comme « à renseigner », sans fabriquer une année d’après les dates ni cacher la période historique. Pour une fenêtre à borne unique autorisée, corriger l’affichage actuel qui peut afficher « Non définie » quand seul le début est absent.

Les contrôles de formulaire facilitent l’usage mais ne remplacent jamais les règles serveur, notamment lorsque la période est fermée après le chargement de l’écran. Un 409 doit conserver le formulaire et permettre de recharger les données ; pas de succès affiché après refus.

## 8. Stratégie éventuelle de migration Flyway V3

### Extension minimale recommandée

**Une migration est nécessaire pour persister `periode.annee_scolaire` : la colonne n’existe pas.** Aucun fichier V3 n’est créé dans cet audit. La future V3 devrait se limiter, dans sa forme la plus compatible, à **ajouter `annee_scolaire VARCHAR(20) NULL`, sans DEFAULT**, sans renumérotation et sans modification de dates, références ou périodes superposées. L’ancien code peut alors continuer à lire/écrire ; cette compatibilité implique toutefois qu’il peut encore produire du NULL.

Les ajouts `evaluation.motif_exception TEXT NULL` et `note.motif_exception TEXT NULL` sont uniquement des options si D5 est décidée et explicitement intégrée au périmètre. Ne pas ajouter dans V3 les champs de versionnement des bulletins, habilitations de compte ou le retrait du coefficient historique. Aucune extension `btree_gist`, EXCLUDE, unicité année seule ou année/libellé n’est justifiée aujourd’hui.

### Reprise et déploiement par étapes

1. Avant toute future migration : sauvegarde complète vérifiée, restauration PostgreSQL 17 isolée, contrôle de V1/V2 et nouveau relevé des données. Arrêter les écrivains concernés pendant la reprise finale ou démontrer une synchronisation équivalente, y compris les changements d’année des classes.
2. Étendre le schéma puis déployer un mapping tolérant les NULL historiques, renseigner explicitement l’année sur les nouvelles créations selon le contrat approuvé. Préserver GET/PUT et l’affichage transitoire ; décider le comportement d’un ancien client sans année, sans déduction silencieuse.
3. Construire les sources par union des années des classes d’évaluations **et** des scolarités de bulletins. Pour chaque période : zéro source = décision manuelle ; plusieurs valeurs = contradiction bloquante ; une valeur = candidate seulement si elle est valide et conforme au calendrier métier approuvé. Vérifier aussi une valeur déjà renseignée : ne jamais l’écraser si elle contredit les références.
4. Sur l’état audité, les trois candidates sont `17/20/28 → 2026-2027`. Obtenir l’approbation D10. Aucune reprise par mois, libellé, première évaluation, MIN/MAX non précédé d’une preuve d’unicité, année actuelle ou valeur par défaut. Conserver une trace de la correspondance approuvée et des contrôles.
5. Effectuer le backfill dans une migration ultérieure autorisée ou dans une V3 explicitement élargie et validée avant publication. Ne pas rééditer une V3 déjà appliquée. L’hypothèse d’une V3 complète ne permet pas de traiter arbitrairement de futures bases ambiguës : elles doivent échouer proprement ou rester en extension nullable selon le contrat retenu.
6. Après reprise et arrêt des anciens écrivains : vérifier zéro NULL/valeur invalide, correspondance des années et protection de toutes les mutations ; seulement alors prévoir NOT NULL et les CHECK approuvés dans une étape distincte. Les numéros futurs dépendront des lots réellement retenus.

### Contraintes, verrous et conservation

Les CHECK actuels d’ordre des dates sont compatibles et restent inchangés. Un futur contrôle local de format/succession nullable est compatible avec une extension vide puis avec les candidates vérifiées ; la nullabilité SQL doit rester distincte de la validation du contenu. Si le contrôle effectue une conversion en entier, prévoir un `CASE` qui protège réellement cette conversion après validation du format, sans supposer un ordre d’évaluation de `AND`. Ne pas contraindre l’année de CLASSE sans vérifier toutes ses valeurs au déploiement.

Une exigence de deux bornes de saisie ensemble serait compatible avec les trois NULL/NULL actuels, mais **pas encore justifiée par une décision métier**. Une interdiction globale des recouvrements est au contraire incompatible avec les données actuelles. Les règles entre tables restent à traiter comme telles.

L’ajout de colonne et les contraintes peuvent prendre des verrous de table ; prévoir une fenêtre courte, des délais explicites et un essai sur copie. `NOT VALID` permet de différer la vérification de certaines nouvelles CHECK/FK, mais ne désactive pas leur contrôle sur les nouvelles écritures ; ce mécanisme ne s’applique pas à toutes les contraintes, notamment à UNIQUE. Un futur NOT NULL exige un contrôle adapté avant activation. [PostgreSQL 17 — ALTER TABLE](https://www.postgresql.org/docs/17/sql-altertable.html).

Avant/après : mêmes PK, FK, relations et colonnes d’origine ; même contenu des notes, dates de saisie, barèmes, coefficients, comptes, profils et PONDERER ; mêmes séquences ; seules la nouvelle colonne et les lignes Flyway attendues peuvent différer. Comparer la **projection des colonnes d’origine**, car le SHA d’un `SELECT *` changera normalement après ajout du champ. Garder les trois identifiants de période, leurs évaluations et leurs notes.

### Retour arrière

Si la transaction de migration échoue avant commit, vérifier son rollback et le retour au schéma/aux données précédents sur copie ; ne pas réparer l’historique Flyway ou relancer aveuglément. Après une extension réussie, le retour applicatif le plus prudent conserve la colonne ajoutée et ses données : l’ancien mapping est à tester avec cette colonne supplémentaire. Une ancienne application peut de nouveau écrire des années NULL, et ne respecte pas les nouveaux contrôles transversaux ; elle ne peut donc pas coexister indéfiniment avec un NOT NULL ou les nouvelles règles.

Ne pas proposer comme rollback standard la suppression de la colonne, une baisse manuelle de version Flyway ou la restauration destructive de la base réelle. Une restauration complète nécessiterait une décision opérationnelle et le traitement des écritures postérieures à la sauvegarde. Préférer une correction en avant quand l’extension a été utilisée ; aucun retour arrière n’est exécuté par cet audit.

## 9. Tests existants, manques et plan automatisé

### Couverture observée et fichiers à adapter

Sous `backend/src/test/java/fr/afpa/backend/` :

- `service/PeriodeServiceTest.java` : **8 tests Mockito** de CRUD/absence ; pas de transactions SQL réelles, année, dépendances, chevauchement ou fermeture.
- `controller/PeriodeControllerTest.java` : **9 tests**, CRUD/404/champs obligatoires/dates inversées de période et de saisie ; couche service mockée. Pas de preuve des protections FK ou de concurrence.
- Usages directs de PERIODE/DTO à adapter : `service/EvaluationServiceTest.java`, `service/BulletinCalculServiceTest.java`, `controller/EvaluationControllerTest.java`, `controller/BulletinControllerTest.java`, `security/AuthorizationHttpTest.java`, en plus des deux fichiers précédents. Les fixtures de constructeurs/JSON doivent recevoir l’année explicitement selon le contrat.
- Fixtures SQL à examiner aussi : `integration/PersonneProfilsIntegrationTest.java` et `migration/PondererMigrationTest.java` insèrent des périodes sans année. Ce dernier migre actuellement vers la dernière version sans cible explicite, attend une seule migration après V1 et compare des lignes sérialisées complètes : une V3 rendrait ces attentes obsolètes. Conserver un test ciblé V1→V2 et ajouter les tests V2→V3 avec projection des colonnes d’origine ; ne pas supprimer les assertions de préservation pour faire passer la nouvelle version.
- Tests de règles dépendantes : `service/NoteServiceTest.java`, `ScolariteServiceTest.java`, `InscriptionServiceTest.java`, `EnseignementServiceTest.java`, `ClasseServiceTest.java`, `BulletinPdfServiceTest.java` ; `controller/NoteControllerTest.java`, `ScolariteControllerTest.java`, `InscriptionControllerTest.java`, `EnseignementControllerTest.java`, `ClasseControllerTest.java`.
- Régression : `integration/PondererIntegrationTest.java`, `migration/PondererMigrationTest.java`, `integration/PersonneProfilsIntegrationTest.java`, `ProfilsConcurrenceIntegrationTest.java`, `ElevePhotoTransactionIntegrationTest.java` ; `service/PondererServiceTest.java`, `ElevePhotoServiceTest.java`, `CompteUtilisateurServiceTest.java`, `FirstAdminBootstrapTest.java` ; `security/SecurityIntegrationTest.java`, `SecurityExpressionsTest.java`, `ActiveAccountJwtValidatorTest.java`, `JwtServiceTest.java`.

`BulletinCalculServiceTest` couvre déjà l’enseignement hors période, l’agrégation multi-enseignants par matière, la lecture de PONDERER plutôt que du champ historique, la pondération absente, les barèmes, l’absence de notes et les arrondis. Il ne prouve pas la compatibilité d’années différentes ou les mutations historiques.

Frontend : `frontend/src/test/roles.test.jsx`, `ponderations.test.jsx`, `profils.test.jsx` restent des régressions ; aucun test Vitest dédié à PERIODE n’est présent. `frontend/e2e/admin-periodes.spec.js` couvre un CRUD autorisé, avec fenêtre renseignée et période supprimable ; pas de dépendances ni cas temporels négatifs. Adapter aussi les fixtures de `admin-evaluations.spec.js`, `admin-notes.spec.js`, `admin-bulletins.spec.js`, `roles-access.spec.js`. Vérifier en régression `admin-classes.spec.js`, `admin-enseignements.spec.js`, `admin-scolarites.spec.js`, `profils-personne.spec.js` et les autres parcours existants. Des fichiers nouveaux de tests année/temporalité/migration pourront être créés pendant l’implémentation, pas ici.

### Scénarios indispensables

| Famille | Scénarios et preuve attendue |
|---|---|
| Année et API | Création/mise à jour/lecture/filtre ; année manquante selon phase ; format invalide, espaces, années non consécutives, limites ; périodes homonymes d’années différentes ; aucun défaut silencieux. |
| Dates | Début=fin, fin antérieure ; toutes combinaisons de bornes de fenêtre ; jour exact d’ouverture/fermeture, veille/lendemain ; période/calendrier hors bornes si règle décidée ; fins NULL ; année bissextile. |
| Temps réel | `Clock` fixe, fuseau métier, minuit et changement d’heure ; modification/suppression de note contrôlée à l’instant courant malgré une ancienne `dateSaisie`. |
| Évaluations/notes | Date aux bornes/hors période, enseignement ou scolarité ; année classe/période incompatible ; classe et barème ; pré-saisie selon D9 ; statut/fenêtre/exception selon matrice, sans permission implicite. |
| Parents | Changer date/année/statut/fenêtre de période, année de classe, dates/classe/matière d’enseignement, date/barème/rattachement d’évaluation, élève/classe/dates de scolarité ; vérifier les enfants et le rollback complet en cas de refus. |
| Suppression | Période vide supprimable ; avec évaluation sans note refusée ; avec notes refusée ; avec bulletin sans évaluation refusée ; 409 métier ; aucune cascade ni modification des références. |
| Recouvrements | Les trois périodes actuelles restent distinctes ; périodes adjacentes et borne commune selon convention inclusive ; même année/différentes années ; règle réellement décidée, pas un rejet générique inventé. |
| Inscription/mobilité | 3 scolarités sans inscription préservées ; couverture absente/partielle/multiple selon décision ; transfert et simultanéité ; pas de fausse donnée historique. |
| JPA et concurrence | Sauvegarde, `flush`, `clear`, relecture ; vraie fermeture concurrente à une saisie ; modification de parent concurrente ; suppression contre création d’enfant ; déplacement entre périodes et, si pertinent, créations concurrentes dans un calendrier vide. Deux connexions physiques et attentes observées, pas seulement des mocks/délais. |
| Migration | Base neuve V1→V2→future V3 ; restauration réelle isolée ; sources uniques/absentes/contradictoires des deux chemins ; année source invalide ; valeur déjà remplie en conflit ; échec atomique ; aucune modification V1/V2/identifiants/séquences. |
| Bulletins et PDF | Résultats identiques à données compatibles, mêmes arrondis/PONDERER ; rejet explicite de combinaison incompatible ; consultation historique après fermeture ; absence de notes ; référence `idMatiere` et profils multiples conservés ; aucune écriture sur GET/PDF. |
| Sécurité | ADMIN/ENSEIGNANT/RESPONSABLE/inactif/non authentifié ; restrictions HTTP et propriété conservées ; année demandée ne donne aucun accès ; double profil sans droit supplémentaire ; dérogation refusée sans autorité décidée. |
| React/Playwright | Année et dates visibles, filtre, options dépendantes, aucune période incompatible préselectionnée, anciens NULL visibles, erreur 400/409 affichée, formulaire préservé, changement d’année efface un résultat obsolète ; parcours notes/bulletins/PDF et CRUD admin. |

### Isolation et non-régression

La situation a évolué depuis l’audit initial : `backend/src/test/resources/application.yaml` possède désormais une datasource de test fermée par défaut ; `support/IsolatedPostgres.java` impose l’URL de test dédiée et refuse le port 5432 ; `scripts/test-ponderer.sh` et `scripts/test-e2e.sh` créent des PostgreSQL temporaires, ports distincts et stockage tmpfs. La CI `.github/workflows/ci.yml` les utilise. Ces scripts ont été **lus mais pas exécutés** ici. Pour les prochaines campagnes, vérifier les comptes applicatifs non superutilisateurs dans les conteneurs jetables et la neutralisation du bootstrap, des secrets et des répertoires uploads réels ; le rôle d’amorçage d’un conteneur de test n’est pas un compte de développement à réutiliser.

Effectuer les tests sur une copie fidèle des sources et PostgreSQL 17 isolé, jamais sur l’URL de développement, sans montage des volumes/photos existants. Une restauration sensible reste privée ; utiliser une seconde copie jetable pour les écritures, car les séquences ne sont pas compensées par le seul rollback transactionnel. Vérifier V1/V2→V3, `ddl-auto=validate`, commits/rollbacks réels, puis suites Java, lint/Vitest/build et Playwright complètes. Les volumes/logs synthétiques sont les seuls artefacts nettoyables.

Repère antérieur du commit fusionné : **421 tests Java, 16 tests React, 15 scénarios Playwright**, avec compilation/lint/build et CI verts. **Aucun de ces tests n’a été relancé pendant cet audit documentaire.** Les tests planifiés ne constituent donc pas une validation d’une future V3.

## 10. Risques, précautions et critères de sortie de l’audit

| Risque | Priorité | Précaution |
|---|---|---|
| Attribuer une année erronée ou fusionner des périodes par dates | Bloquant avant reprise | Sources concordantes des deux chemins, approbation D10, conserver IDs et données. |
| Interdire les recouvrements malgré les 3 paires existantes | Bloquant avant contrainte | D2, aucun EXCLUDE/UNIQUE ajouté opportunément. |
| Déployer un champ obligatoire avant reprise/arrêt des anciens clients | Bloquant avant bascule | Extension nullable puis coordination ; pas de DEFAULT arbitraire. |
| Croire qu’un contrôle de création protège les modifications de parents | Important | Revalidation ou gel ; tests de concurrence et protocole de verrous sur tous les chemins concernés. |
| Fermer les périodes historiques ou rendre rétroactivement des notes invalides | Important | Définir portée prospective, pré-saisie, fenêtres NULL et accès historique. |
| Changer les bulletins par filtrage silencieux ou suppression de données | Important | Erreurs explicites et conservation du calcul/PDF courant ; versionnement hors périmètre. |
| Dérégler les accès en ajoutant une notion d’année | Important | Les rôles/propriétés restent contrôlés côté serveur ; aucune permission tirée des profils multiples. |
| Écrire sur PostgreSQL réel via test, Flyway ou bootstrap | Bloquant opérationnel | Aucun lancement pendant l’audit ; prochaines exécutions sur restauration et stockage isolés seulement. |
| Promettre un retour arrière destructif sans traiter les écritures nouvelles | Important | Conserver l’extension, tester retour applicatif, privilégier correction en avant. |

Les lots 1 et 2 et le correctif photo ne requièrent aucune modification pour produire cet audit. Le fichier local **`docs/merise/LoopingImage.jpg`** reste volontairement non suivi ; relevé initial : **143 365 octets**, SHA-256 **`57dfcfa13983c3ffd577bda58ad86bb9fade18ce325e040f413ce6dcde0396e2`**. Il n’est ni ajouté à Git, ni déplacé, ni renommé, ni réécrit ; `.gitignore` reste inchangé.

### Contrôles de préservation exécutés

- Manifeste final SQL strictement identique au manifeste initial : données et PK des 18 tables, état des 12 séquences, métadonnées et historique Flyway. PostgreSQL reste healthy ; le compte ADMIN actif est conservé. Les lectures ne prétendent pas figer les fichiers internes d’un serveur PostgreSQL vivant.
- V1 conserve le SHA-256 `f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee` ; V2 conserve `35734eef70167a37c7f97749f364b88bdd10b6c7396df1c8a36e41548adb25f9`. Aucun autre fichier de migration n’existe.
- Fichiers suivis inchangés, index Git inchangé, branche et HEAD de référence conservés. Seul nouveau livrable du projet : ce document, laissé non indexé. L’image autorisée reste non suivie, avec taille et SHA-256 strictement identiques avant/après. Aucun accès aux photos ou sauvegardes n’est nécessaire à cet audit ; aucune opération sur les volumes Docker n’est effectuée.

**Conclusion de préparation :** le lot 3 peut être conçu à partir de l’ajout d’année et de contrôles temporels ciblés. Il ne peut pas être développé intégralement sans arbitrer le format/calendrier scolaire, la portée des périodes et des recouvrements, les fenêtres/statuts, les modifications historiques, la reprise des trois années et la transition API. Définir explicitement si inscriptions obligatoires, dérogations, pré-saisie et mobilité entrent dans ce premier incrément. **Une migration additive sera nécessaire pour l’année ; aucune V3 n’est créée ou exécutée par cet audit.**
