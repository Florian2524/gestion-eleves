# Lot 3A — Implémentation de l’année scolaire des périodes

Travail du **4 novembre 2026**, branche **`feature/merise-lot3-annee-scolaire`**, depuis `main` / `353f9fdc1466556ee07361d1204ab09d0eaa5402`. Ce document décrit le livrable préparé pour publication et revue ; aucune fusion ni migration réelle n'est autorisée par cette publication.

L’[audit du lot 3](LOT3_AUDIT_PERIODES.md) est conservé intégralement. La décision explicite du lot 3A retient **VARCHAR(9)**, à la place de VARCHAR(20) proposé par l’audit. Le champ reste facultatif : **pas de défaut, pas de backfill, pas de nouvelle règle temporelle**.

## 1. Périmètre et choix réalisés

Une période peut porter une chaîne `anneeScolaire` au format canonique `AAAA-AAAA`, avec deux groupes de quatre chiffres ASCII et seconde année = première + 1. Exemple : `2026-2027`.

NULL signifie « année scolaire non renseignée ». Le backend accepte NULL ; une chaîne vide, des espaces, un séparateur différent, des chiffres Unicode ou des années non consécutives sont invalides. Aucune année n’est déduite des dates, du libellé, de la classe ou de l’année courante. La saisie vide du formulaire est convertie explicitement en NULL.

Aucune borne calendaire supplémentaire n’est inventée : `0000-0001` et `9998-9999` satisfont la règle numérique retenue ; `9999-0000` ne la satisfait pas. La définition d’un calendrier scolaire effectif reste séparée. Les dates 2026 avec une année 2027-2028 restent acceptées dans ce sous-lot : la cohérence intertables et entre année/dates relève du lot 3B.

Le calcul des bulletins, les notes, les évaluations, les coefficients PONDERER, la composition des profils et les transactions de photos ne sont pas modifiés. Pas de filtre d’année, fermeture de période, dérogation, contrainte de chevauchement ou versionnement de bulletin ajouté.

## 2. Flyway V3 : SQL exact

Fichier : [V3__add_annee_scolaire_periode.sql](../../backend/src/main/resources/db/migration/V3__add_annee_scolaire_periode.sql).

```sql
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
```

Le CASE protège les conversions en INTEGER : un format invalide est refusé sans supposer l’ordre d’évaluation d’un AND. Le CHECK autorise NULL et n’accède à aucune autre table. Aucun UPDATE/INSERT métier, index, table, FK, identifiant ou séquence n’est ajouté ou modifié par V3.

L’idempotence demandée est celle de **Flyway** : V3 s’exécute une fois, la seconde commande migrate applique zéro migration. Le SQL n’a pas vocation à être réexécuté manuellement ou rendu silencieusement répétable avec IF NOT EXISTS.

V1 et V2 sont inchangées, SHA-256 :

- V1 : `f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee`.
- V2 : `35734eef70167a37c7f97749f364b88bdd10b6c7396df1c8a36e41548adb25f9`.

### Limite standard de VARCHAR(9)

Le CHECK valide la valeur **après conversion au type de la colonne**. PostgreSQL peut supprimer des espaces finaux excédant la longueur VARCHAR, et un cast explicite vers VARCHAR(n) peut tronquer son entrée avant le CHECK. Une entrée SQL brute `2026-2027 ` peut donc devenir la valeur stockée canonique `2026-2027`. L’API Java refuse cette entrée brute non canonique ; les valeurs stockées acceptées respectent le même format et la même règle de succession que Java. Un test explicite documente cette différence de coercition SQL ; elle n’est ni masquée ni présentée comme un contrôle du texte SQL avant conversion. [PostgreSQL 17 — types caractères](https://www.postgresql.org/docs/17/datatype-character.html).

## 3. JPA, DTO, services et API

- `Periode` possède `String anneeScolaire`, `@Column(name="annee_scolaire", length=9)`, nullable. Le constructeur existant reste utilisable et laisse cette propriété à NULL.
- `PeriodeRequest` ajoute le composant facultatif `anneeScolaire`. Un `@AssertTrue` / `@JsonIgnore` vérifie le format et les années consécutives. Le même prédicat est appelé par `PeriodeService.create/update` pour protéger les appels Java directs, avant écriture. Message : « L’année scolaire doit respecter le format AAAA-AAAA avec deux années consécutives (exemple : 2026-2027). »
- Le mapper renseigne la propriété à la création et, lors d'une modification, uniquement si elle figure dans le JSON ; il l’expose dans `PeriodeResponse`. Aucun enrichissement automatique depuis les relations existantes.
- `PeriodeRepository` et `PeriodeController` ont été examinés : leurs CRUD et routes existants suffisent, aucune modification nécessaire. Aucun paramètre de filtre supplémentaire n’est introduit par opportunité.
- `ddl-auto: validate` et la configuration de sécurité restent inchangés.

Contrats HTTP conservés : GET `/periodes`, GET `/periodes/{id}`, POST `/periodes`, PUT `/periodes/{id}`, DELETE `/periodes/{id}` ; mêmes codes de succès et mêmes permissions. Les GET exposent une propriété supplémentaire `anneeScolaire`, chaîne ou NULL. Les anciens JSON sans ce champ sont acceptés.

**Contrat de PUT arrêté lors de la revue finale : année omise → valeur existante conservée ; `anneeScolaire: null` explicite → effacement ; chaîne valide explicite → remplacement.** Les autres champs gardent leur comportement de mise à jour antérieur. Le POST sans année crée une période avec NULL.

La première implémentation assimilait champ omis et NULL et pouvait effacer une année renseignée lors d'un PUT d'ancien client. La revue a corrigé ce risque avant publication : `PeriodeRequestDeserializer` délègue les conversions de valeurs à Jackson et relève uniquement la présence effective de la propriété. Cette information interne est ignorée en sortie JSON et ne peut pas être imposée par un indicateur du client. Le constructeur Java à sept arguments reste disponible et traite sa valeur comme explicitement fournie, y compris NULL. Ce choix de compatibilité additive concerne seulement le nouveau champ ; il ne crée pas d'API PATCH et ne change pas les autres ressources.

Un format ou une succession incorrects donnent HTTP 400. La validation de requête produit un détail `validationErrors.anneeScolaireValide` ; le service réutilise la même explication dans `BusinessRuleViolationException`. Les flags de validation ne sont pas des champs JSON. Les clients stricts sur les propriétés supplémentaires doivent accepter le nouveau champ de réponse.

Les GET restent accessibles à ADMIN, ENSEIGNANT et RESPONSABLE ; les écritures de périodes restent ADMIN seulement. Aucun rôle, claim JWT, permission de profil ou compte existant n’est changé. Une erreur de saisie d’année ne modifie pas les notes, périodes ou comptes.

## 4. React

L’administration des périodes propose un champ texte facultatif « Année scolaire », limité à neuf caractères et initialisé vide. La validation locale contrôle le même format et la même succession que Java. La colonne affiche l’année ou **« Année scolaire non renseignée »**.

Les labels de période utilisés par les évaluations, les notes et le sélecteur des bulletins incluent l’année ou ce même libellé explicite pour NULL/absence de propriété. Les valeurs de sélection restent les identifiants existants ; aucun tri, filtre, présélection ou règle d’autorisation temporelle n’est ajouté.

Le composant partagé `ResourcePage` conserve la conversion d’une chaîne facultative vide vers NULL. Il affiche maintenant les messages détaillés `validationErrors` renvoyés par le backend, avec repli sur le message général ; le formulaire est conservé après refus. Aucun changement du transport HTTP ou des routes API n’est nécessaire.

## 5. Fichiers du lot

Sous `backend/src/main/java/fr/afpa/backend/`, fichiers modifiés :

- `entity/Periode.java`.
- `dto/periode/PeriodeRequest.java`, `PeriodeResponse.java`.
- Ajout `dto/periode/PeriodeRequestDeserializer.java`, pour la distinction entre absence et NULL explicite.
- `mapper/PeriodeMapper.java`.
- `service/PeriodeService.java`.

Migration ajoutée :

- `backend/src/main/resources/db/migration/V3__add_annee_scolaire_periode.sql`.

Sous `backend/src/test/java/fr/afpa/backend/` :

- Ajouts : `migration/PeriodeAnneeScolaireMigrationTest.java`, `integration/PeriodeAnneeScolaireIntegrationTest.java`.
- Adaptations : `migration/PondererMigrationTest.java` cible explicitement V2 et conserve ses assertions de préservation ; `integration/PersonneProfilsIntegrationTest.java` attend V1/V2/V3 sur le schéma courant et continue de vérifier les checksums V1/V2 ainsi que les PK/FK de profils ; `controller/PeriodeControllerTest.java` et `service/PeriodeServiceTest.java` conservent leurs scénarios avec année NULL.

Frontend :

- Modifiés : `frontend/src/pages/AdministrationPages.jsx`, `BulletinsPage.jsx`, `frontend/src/components/school/ResourcePage.jsx`, `schoolData.js`, `frontend/e2e/admin-periodes.spec.js`.
- Ajoutés : `frontend/src/components/school/schoolYear.js`, `frontend/src/test/periodes.test.jsx`.

Infrastructure de test, adaptation justifiée par l’exigence de rôle SQL non superutilisateur :

- `scripts/test-ponderer.sh` et `scripts/test-e2e.sh` séparent désormais l’administrateur d’amorçage du cluster **jetable** et le rôle applicatif propriétaire de sa seule base de test. Un SELECT vérifie que ce dernier n’est ni superutilisateur ni autorisé à créer des bases/rôles. Flyway et Spring utilisent seulement ce rôle applicatif. Aucun privilège local existant n’est touché.
- Le lanceur Java possède aussi son répertoire uploads temporaire par exécution. Les conteneurs restent en tmpfs et sur ports aléatoires de loopback ; seuls les conteneurs et répertoires créés par l’exécution sont nettoyés.

Documentation : cet écrit et `docs/merise/LOT3_AUDIT_PERIODES.md`, conservé intégralement et destiné à une future publication explicite. **`docs/merise/LoopingImage.jpg` est exclu de cette liste de publication et reste non suivi.** Aucun .env, upload, dump, secret réel ou rapport privé de tests n’est ajouté au dépôt.

## 6. Validation automatisée sur PostgreSQL isolé

Les sources actuelles, y compris les nouveaux fichiers non commités nécessaires, sont copiées dans un répertoire privé sous `/tmp`. Les essais utilisent PostgreSQL **17.10**, stockage tmpfs sans montage d’un volume existant, ports distincts de 5432, rôles applicatifs non superutilisateurs et uploads temporaires. Le compte d’amorçage ne sert qu’à créer les bases/rôles de ces conteneurs jetables. Les dépendances frontend ont été installées avec `npm ci --offline` sur la copie.

| Vérification | Résultat |
|---|---|
| Suite Java complète | **438 tests réussis**, zéro échec/erreur/test ignoré, contre 421 auparavant. |
| Compilation et packaging Maven | Réussis ; compilation des sources et tests, jar Spring Boot produit sur copie. |
| Vitest | **27 tests réussis**, contre 16 auparavant. |
| ESLint | Réussi. |
| Build frontend de production | Réussi. |
| Playwright complet | **16 scénarios réussis**, zéro échec, ignoré ou flaky, contre 15 auparavant. |
| Lanceur Java du dépôt avec rôles séparés | **14 tests V3 ciblés réussis** lors de l’implémentation ; exécution réelle sur son propre conteneur jetable, avant les trois régressions ajoutées en revue. |
| Lanceur E2E du dépôt avec rôles séparés | **2 parcours périodes réussis**, avec création/modification/effacement et compatibilité NULL. |

La première suite Java a révélé une attente ancienne « V1/V2 seulement » du test de profils et une erreur de requête de lecture de séquences dans le nouveau test de migration. L’attente a été adaptée à V3 sans retirer les contrôles V1/V2 ; la lecture utilise explicitement `last_value`/`is_called`. La suite complète a ensuite été relancée et passe. Aucune règle métier extérieure au lot 3A n’a été changée pour contourner un échec.

Les **17 tests Java ajoutés** couvrent installation V1→V2→V3, état des colonnes, absence de défaut, données historiques, séquences/contraintes, idempotence Flyway, NULL et formats valides/invalides, parité des valeurs SQL/Java et coercition VARCHAR. L’intégration traverse de vrais commits puis des `flush`, `clear` et relectures pour création, modification et effacement ; les API réelles sont exercées par MockMvc, avec anciens JSON, préservation de l’année omise, effacement explicite, refus des faux indicateurs internes, HTTP 400 et droits des trois rôles. Le schéma chargé par Hibernate est V3.

Les tests frontend couvrent affichage NULL/année, création, modification, effacement, erreurs locales, erreurs détaillées du serveur, transport de chaînes/NULL et labels. Playwright vérifie le CRUD avec année, sa modification/effacement et une création historique NULL avec refus des formats et successions invalides. Les suites profils multiples, PONDERER, photos transactionnelles, notes, bulletins/PDF, JWT et comptes restent présentes et vertes dans les suites complètes.

## 7. Validation sur restauration réelle isolée déjà exécutée

Archive utilisée **en lecture uniquement**, conservée :

`/home/florian/backups/gestion-eleves/2026-10-27T115420Z-lot2-742622ef/gestion_eleves.dump`

SHA-256 vérifié : `e930159b25b575315d7f0c8b60e0dcfe5e0c1b02291b77ba5717ab716e8e8c82`.

Le catalogue de l’archive a été lu puis la restauration a réussi dans **`lot3a_restore`**, base d’un conteneur temporaire distinct. Restauration sans propriétaire/ACL d’origine, objets possédés par le rôle applicatif non superutilisateur de la copie. Les données/PK des **18 tables**, les **12 séquences** et l’historique V1/V2 restaurés correspondent au dernier instantané validé du 28 octobre. Cela ne prouve pas l’état SQL actuel du développement arrêté depuis le 31 octobre.

Sur cette restauration :

1. Ancien backend de référence : Hibernate validate, compte ADMIN existant présent, 3 périodes chargées, **9 calculs et 9 PDF** produits sans écriture métier.
2. Prévalidation Flyway explicitement à V2, puis application de **V3 seule**, validation de la chaîne et seconde migration avec **zéro exécution**. La prévalidation ne doit pas exiger que V3 soit déjà appliquée avant sa migration.
3. Nouveau backend : Hibernate validate sur V3, bootstrap neutralisé, loopback et port temporaire ; périodes et compte ADMIN chargés ; **9 calculs et 9 PDF** produits. Les neuf résultats de calcul structurés sont strictement identiques avant/après. Les PDF ont une signature valide et une taille non vide ; leur identité binaire n’est pas revendiquée, notamment à cause de métadonnées de génération.
4. Comparaison intégrale : projections des anciennes colonnes/PK des 18 tables, 12 séquences et toutes les métadonnées non concernées identiques. Seuls s’ajoutent `periode.annee_scolaire`, son CHECK et la ligne d’historique Flyway V3 ; les lignes V1/V2 restent identiques.
5. Les périodes **17, 20, 28** gardent **NULL** dans la nouvelle colonne. Leurs identifiants, dates, libellés, statuts, évaluations et notes sont préservés. Les chevauchements, scolarités sans inscription et pré-saisies de notes ne sont pas corrigés.

La vérification ne se réduit pas à un nombre de lignes : empreintes SHA-256 des données et des PK, états de séquences et catalogues sont comparés. La nouvelle colonne est projetée hors des anciennes lignes de PERIODE pour comparer des schémas différents. Les erreurs de nettoyage des photos ne sont pas provoquées sur cette copie sensible ; leurs scénarios restent couverts sur les fixtures synthétiques des tests dédiés.

## 8. État réel du développement et préservation

**Limite constatée avant l’implémentation :** le conteneur `gestion-eleves-postgres` est **arrêté depuis le 31/10/2026 à 16:49:23 UTC** et son état Docker n’est pas healthy. Il n’a pas été démarré, redémarré, restauré ni utilisé par les applications/tests de cette campagne. Le contrôle initial s’est arrêté sur ce constat avant toute connexion SQL réelle. La branche de travail avait déjà été créée à ce stade ; aucune modification source n’avait encore été effectuée.

En conséquence, il est impossible de certifier par SQL pendant cette campagne « PostgreSQL réel healthy, toujours en V2, données/séquences avant-après identiques ». La dernière preuve SQL de V2 reste celle du 28 octobre. La présence de V3 dans les sources **n’autorise pas à l’appliquer sur la base réelle**. Ne pas présenter la restauration de l’archive comme la dernière base au 31 octobre.

Les empreintes et métadonnées de l’image, de l’audit, des dossiers uploads et des sauvegardes ont été relevées avant le travail ; les identifiants/montages/état du conteneur et l’inventaire des volumes sont conservés pour comparaison finale. Aucun contenu du volume de développement n’est monté par les essais. Les copies sensibles et journaux restent dans un répertoire privé hors Git.

Contrôle final de la campagne d’implémentation, avant publication : **148 fichiers de sauvegarde**, image, audit et état des dossiers uploads strictement identiques aux relevés initiaux ; identifiant, image, montages, dates de démarrage/arrêt du conteneur de développement et inventaire des volumes inchangés. Tous les backends Java de test sont arrêtés ; les conteneurs de test créés par la campagne sont supprimés, exclusivement avec leur stockage tmpfs. Aucun processus Java ne subsiste lors du contrôle. Aucun SQL n’a été exécuté sur la base réelle. Les fichiers suivis ne présentent que les modifications prévues ; l’index Git est vide et le HEAD reste le commit de référence.

Fichiers locaux à conserver :

| Fichier | Taille et SHA-256 attendus |
|---|---|
| `docs/merise/LoopingImage.jpg` | **143365 octets**, `57dfcfa13983c3ffd577bda58ad86bb9fade18ce325e040f413ce6dcde0396e2`, volontairement non suivi. |
| `docs/merise/LOT3_AUDIT_PERIODES.md` | **55985 octets**, `6891bb0446398d2b1ffbf9f84b86d60466f5f1dd49f51fe448e09eec3d1e4c56`, identique à la copie de préservation. |

Copie préexistante du rapport : `/tmp/gestion-eleves-lot3a-preservation-nbmuhjrh/LOT3_AUDIT_PERIODES.md`. Les deux originaux restent à leur place. Le rapport fait partie du livrable documentaire à indexer explicitement pour cette publication ; l’image doit rester hors Git. Aucune affirmation de continuité historique du JPEG entre deux relevés n’est nécessaire : les empreintes concernent cette campagne.

## 9. Déploiement futur et retour applicatif

1. Faire rétablir et vérifier PostgreSQL réel dans une étape distincte et explicitement contrôlée. Ne pas lancer ce nouveau backend : avec Flyway activé par défaut, il appliquerait V3 au démarrage.
2. Relever l’état SQL actuel en lecture seule : V1/V2 et checksums, données, PK/FK, séquences, comptes et périodes ; vérifier qu’aucune V3 étrangère n’existe. Si l’état a changé depuis l’archive testée, créer une nouvelle sauvegarde cohérente lors de cette étape autorisée et la restaurer sur un nouveau PostgreSQL 17 isolé.
3. Rejouer V2→V3 et les comparaisons complètes sur cette restauration récente, puis `ddl-auto=validate`, lectures API, bulletins/PDF et essais de mutation sur une copie jetable distincte. Vérifier l’absence de backfill, notamment des périodes 17/20/28 si elles existent encore.
4. Après revue, commit/PR/CI et autorisation explicite de migration réelle : arrêter les anciens écrivains/backends, préparer une courte fenêtre pour le verrou ALTER TABLE, appliquer V3 une seule fois via Flyway et vérifier le schéma/historique/valeurs NULL. Aucun SQL manuel improvisé ou modification de V1/V2.
5. Déployer le backend compatible V3 puis l’interface, contrôler les droits et la sémantique PUT. Ne pas rendre le champ obligatoire ou renseigner l’année des périodes historiques dans cette intervention.

V3 reste additive. Un retour à l’ancien backend peut être envisagé après essai sur copie, en laissant la colonne et son CHECK en place ; ses créations de périodes laissent l’année NULL, ses mises à jour JPA ne connaissent pas la colonne supplémentaire. Ne pas supprimer la colonne, réécrire l’historique Flyway, appliquer reset/clean ou restaurer destructivement une sauvegarde en guise de rollback automatique. Le nouveau backend préserve désormais l'année lorsqu'un ancien client l'omet dans un PUT ; seul un NULL explicite l'efface.

## 10. Décisions reportées au lot 3B et conclusion

Restent hors périmètre : cohérence année/date/classe, calendrier et fuseau, fenêtres/statuts/fermeture, contrôles de mutation des parents et concurrence, inscriptions couvrantes, pré-saisie, dérogations et motifs, chevauchements, mobilité et évolution des bulletins officiels. Aucun de ces mécanismes n’est commencé ici.

L’implémentation et les validations isolées de 3A sont réalisées. La publication du code ne dépend pas d'un redémarrage de PostgreSQL réel : son schéma courant reste **non vérifié pendant cette campagne**, conformément à l'interdiction de le démarrer. Toute validation et migration réelle exige une étape séparée et explicitement autorisée. La PR peut être revue sur les preuves isolées ; sa fusion et son déploiement restent des décisions distinctes.
