# Audit Merise → PostgreSQL et préparation Spring Boot

Audit du **26 octobre 2026**, dépôt `Florian2524/gestion-eleves`, branche `main`, commit observé `f09c17430541e2b846ec30b502adb2cb0ddaad18`.

**Conclusion : la migration peut commencer par des ajouts compatibles, mais pas par le remplacement global du schéma.** Les données pédagogiques présentes sont cohérentes sur les contrôles exécutés. Les principaux écarts concernent les pondérations classe–matière, les bulletins persistés/versionnés, leur validation et l’héritage Java. Trois périodes se recouvrent complètement ; aucune inscription n’existe pour les trois scolarités. Ces deux constats ne justifient aucune correction automatique.

## 1. Périmètre, preuves et limites

Sources examinées : [MCD](MCD.png), [MLD](MLD.png), fichier binaire [Looping](gestion-eleves.loo), [ancien MPD](mpd-postgresql.sql), [V1](../../backend/src/main/resources/db/migration/V1__create_initial_schema.sql), toutes les entités/repositories, services, DTO/mappers, contrôleurs, sécurité, tests, interfaces React, scripts et CI.

Les liens backend/frontend ci-dessous sont relatifs à ce rapport. La source Looping est binaire : ses cardinalités n’ont pas été prétendument extraites par un parseur fiable. Les deux PNG ont été inspectés visuellement. Ils présentent les 14 entités et les 3 associations matérialisées en tables, soit **17 relations cibles**. Les 18 associations du MCD sont : POSSÉDER, ASSURER, ÊTRE_RÉFÉRENT, DESTINER, PORTER_SUR, COMPORTER, SITUER, ACCUEILLIR, SUIVRE, OBTENIR, NOTER, INSCRIRE, ÉTABLIR, COUVRIR, VALIDER, RESPONSABILITÉ_LÉGALE, PONDÉRER et LIGNE_BULLETIN.

La règle fournie pour l’héritage, **partiel et recouvrant**, prévaut sur le `XT` temporaire utilisé pour contourner le plantage de Looping. Une personne peut n’avoir aucun profil spécialisé ou en cumuler plusieurs, notamment enseignant et responsable. Aucune contrainte d’exclusion ni de totalité ne doit être déduite de cet export technique.

### Exécution réelle

- PostgreSQL interrogé via `docker compose exec -T ... postgres psql`, base `gestion_eleves`, serveur **17.10 (Debian 17.10-1.pgdg13+1)**.
- Première transaction horodatée par le serveur à **2026-10-26 10:16:31 UTC**, soit **11:16:31 Europe/Paris**.
- Chaque session : `psql -X`, `ON_ERROR_STOP=1`, `default_transaction_read_only=on`, `statement_timeout=15000`, `lock_timeout=3000`, puis `BEGIN ... REPEATABLE READ READ ONLY`. `transaction_read_only=on` a été confirmé. Les transactions ont été terminées sans mutation métier ni DDL ; les `COMMIT` ne contenaient que des lectures.
- Plusieurs transactions successives ont été utilisées : chaque transaction a une vue cohérente, mais l’ensemble ne constitue pas un instantané unique si une autre application écrit simultanément.
- Le premier accès au socket Docker était interdit par le confinement ; les commandes ciblées en lecture seule ont ensuite été autorisées et ont abouti. L’audit PostgreSQL n’est donc pas une simple déduction de V1.
- Aucun démarrage Spring Boot, test, migration, reset, `nextval`, `setval`, modification Docker, commit ou push. Seul ce rapport a été créé.
- Aucun nom, courriel, adresse, hash de mot de passe, commentaire de note ou identité d’élève n’est reproduit. Les identifiants affichés concernent des classes, matières et périodes.
- Les deux blocs SQL des annexes ont été réexécutés intégralement avec succès à **10:29:33 UTC** : lecture seule confirmée, mêmes anomalies et mêmes empreintes pour enseignement, scolarité, évaluation et note. Les liens locaux du rapport ont été vérifiés ; V1 conserve le SHA-256 ci-dessous et le rapport est le seul ajout dans l’état Git final.

Le MPD est **strictement identique octet pour octet à V1**. SHA-256 de V1 : `f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee`. Le CRC32 calculé sur les lignes UTF-8, sans séparateurs, vaut `-526686782`, identique au checksum enregistré dans Flyway. Cette vérification de fichier n’est pas une exécution de Flyway.

Le document [conception-audit.md](../conception-audit.md), daté du 4 octobre, décrit l’ancien état : ses réserves sur les anciennes cardinalités graphiques ne remplacent pas les PNG actuels. Aucun document antérieur n’a été réécrit.

## 2. État réel de PostgreSQL

### 2.1 Volumétrie exhaustive

| Table | Lignes | Enjeu de conservation |
|---|---:|---|
| personne | 8 | Identités, coordonnées, identifiants partagés |
| eleve | 4 | Matricules, naissances, références photo |
| enseignant | 3 | Numéros d’employé, liens pédagogiques |
| responsable | 0 | Structure à conserver |
| compte_utilisateur | 1 | Compte ADMIN actif, identifiant et secret existants |
| classe | 4 | Toutes en `2026-2027` |
| matiere | 3 | Codes et références |
| periode | 3 | Référencées par les évaluations |
| inscription | 0 | Ne pas inventer d’inscriptions historiques |
| scolarite | 3 | Toutes `ACTIVE`, référencées par les notes |
| enseignement | 3 | Tous actifs, futurs coefficients à extraire |
| evaluation | 3 | Barèmes, coefficients et relations à conserver |
| note | 3 | Deux `SAISIE`, une `VALIDEE` |
| bulletin | 0 | Aucun historique persistant actuellement à convertir |
| ligne_bulletin | 0 | Aucun résultat figé actuellement à convertir |
| responsabilite_legale | 0 | Association déjà correctement structurée |
| flyway_schema_history | 1 | V1 réussie, checksum `-526686782` |

Le schéma `public` contient **16 tables métier + l’historique Flyway**, 98 colonnes au total dont 10 dans l’historique, **61 contraintes** (17 PK, 22 FK, 11 UNIQUE, 11 CHECK), **29 index** (28 de PK/UNIQUE et un index `success` de Flyway). Tous les index inspectés sont valides/prêts et toutes les contraintes cataloguées sont validées. Aucune vue `public`, aucun trigger applicatif exposé par `information_schema.triggers`, aucune politique RLS `public`.

### 2.2 Colonnes réellement présentes

Notation : `?` nullable ; toutes les autres colonnes sont NOT NULL. `I` = `BIGINT GENERATED BY DEFAULT AS IDENTITY`, `B` = BIGINT, `N` = NUMERIC(5,2), `D` = DATE, `T` = TIMESTAMP WITH TIME ZONE, `V(n)` = VARCHAR(n). Les PK spécialisées sont des BIGINT partagés, sans séquence propre.

| Table | Colonnes, types et valeurs par défaut |
|---|---|
| personne | `id_personne I`, `nom V(100)`, `prenom V(100)`, `email_contact V(255)?`, `telephone V(30)?`, `adresse V(500)?` |
| compte_utilisateur | `id_utilisateur I`, `id_personne B`, `email_connexion V(255)`, `mot_de_passe_hash V(255)`, `role V(30)`, `actif BOOLEAN DEFAULT true`, `date_creation T DEFAULT CURRENT_TIMESTAMP` |
| eleve | `id_personne B`, `matricule V(50)`, `date_naissance D`, `photo_url V(500)?` |
| enseignant | `id_personne B`, `numero_employe V(50)` |
| responsable | `id_personne B`, `profession V(150)?` |
| classe | `id_classe I`, `id_professeur_principal B?`, `nom V(100)`, `niveau V(50)`, `annee_scolaire V(20)` |
| matiere | `id_matiere I`, `code V(30)`, `nom V(100)` |
| periode | `id_periode I`, `libelle V(100)`, `date_debut D`, `date_fin D`, `date_debut_saisie D?`, `date_fin_saisie D?`, `statut V(30)` |
| inscription | `id_inscription I`, `id_eleve B`, `date_inscription D`, `date_fin D?`, `statut V(30)` |
| scolarite | `id_scolarite I`, `id_eleve B`, `id_classe B`, `date_debut D`, `date_fin D?`, `statut V(30)` |
| enseignement | `id_enseignement I`, `id_enseignant B`, `id_classe B`, `id_matiere B`, `coefficient_matiere N DEFAULT 1`, `date_debut D`, `date_fin D?`, `actif BOOLEAN DEFAULT true` |
| evaluation | `id_evaluation I`, `id_enseignement B`, `id_periode B`, `libelle V(150)`, `date_evaluation D`, `type_evaluation V(50)`, `coefficient_evaluation N DEFAULT 1`, `bareme N DEFAULT 20` |
| note | `id_note I`, `id_scolarite B`, `id_evaluation B`, `valeur N`, `commentaire TEXT?`, `statut_note V(30)`, `date_saisie T DEFAULT CURRENT_TIMESTAMP` |
| bulletin | `id_bulletin I`, `id_scolarite B`, `id_periode B`, `id_validateur B?`, `date_generation D?`, `moyenne_generale N?`, `appreciation_generale TEXT?`, `statut V(30)`, `date_validation T?` |
| ligne_bulletin | `id_ligne_bulletin I`, `id_bulletin B`, `id_enseignement B`, `texte TEXT?`, `date_saisie T?`, `date_modification T?`, `statut V(30)` |
| responsabilite_legale | `id_responsable B`, `id_eleve B` |
| flyway_schema_history | `installed_rank INTEGER`, `version V(50)?`, `description V(200)`, `type V(20)`, `script V(1000)`, `checksum INTEGER?`, `installed_by V(100)`, `installed_on TIMESTAMP WITHOUT TIME ZONE DEFAULT now()`, `execution_time INTEGER`, `success BOOLEAN` |

### 2.3 PK, clés étrangères, uniques et contrôles

Les PK sont les colonnes `id_*` de chaque table, sauf `responsabilite_legale(id_responsable,id_eleve)` et `flyway_schema_history(installed_rank)`.

Les **22 FK** réelles sont les suivantes. Sans mention, la suppression est `NO ACTION`, sans cascade ; les mises à jour des FK sont `NO ACTION`.

| Source | Cible | ON DELETE |
|---|---|---|
| compte_utilisateur.id_personne | personne.id_personne | CASCADE |
| eleve.id_personne | personne.id_personne | CASCADE |
| enseignant.id_personne | personne.id_personne | CASCADE |
| responsable.id_personne | personne.id_personne | CASCADE |
| classe.id_professeur_principal | enseignant.id_personne | SET NULL |
| inscription.id_eleve | eleve.id_personne | NO ACTION |
| scolarite.id_eleve | eleve.id_personne | NO ACTION |
| scolarite.id_classe | classe.id_classe | NO ACTION |
| enseignement.id_enseignant | enseignant.id_personne | NO ACTION |
| enseignement.id_classe | classe.id_classe | NO ACTION |
| enseignement.id_matiere | matiere.id_matiere | NO ACTION |
| evaluation.id_enseignement | enseignement.id_enseignement | NO ACTION |
| evaluation.id_periode | periode.id_periode | NO ACTION |
| note.id_scolarite | scolarite.id_scolarite | NO ACTION |
| note.id_evaluation | evaluation.id_evaluation | NO ACTION |
| bulletin.id_scolarite | scolarite.id_scolarite | NO ACTION |
| bulletin.id_periode | periode.id_periode | NO ACTION |
| bulletin.id_validateur | enseignant.id_personne | SET NULL |
| ligne_bulletin.id_bulletin | bulletin.id_bulletin | CASCADE |
| ligne_bulletin.id_enseignement | enseignement.id_enseignement | NO ACTION |
| responsabilite_legale.id_responsable | responsable.id_personne | CASCADE |
| responsabilite_legale.id_eleve | eleve.id_personne | CASCADE |

UNIQUE réels, avec leurs index B-tree :

| Nom | Colonnes |
|---|---|
| uq_compte_utilisateur_personne | compte_utilisateur(id_personne) |
| uq_compte_utilisateur_email | compte_utilisateur(email_connexion) |
| uq_eleve_matricule | eleve(matricule) |
| uq_enseignant_numero_employe | enseignant(numero_employe) |
| uq_classe_nom_annee | classe(nom,annee_scolaire) |
| uq_matiere_code | matiere(code) |
| uq_scolarite_eleve_classe_debut | scolarite(id_eleve,id_classe,date_debut) |
| uq_enseignement_affectation | enseignement(id_enseignant,id_classe,id_matiere,date_debut) |
| uq_note_scolarite_evaluation | note(id_scolarite,id_evaluation) |
| uq_bulletin_scolarite_periode | bulletin(id_scolarite,id_periode) |
| uq_ligne_bulletin_enseignement | ligne_bulletin(id_bulletin,id_enseignement) |

Les 11 CHECK couvrent : rôle dans `ADMIN/ENSEIGNANT/RESPONSABLE` ; dates ordonnées pour période, fenêtre de saisie, inscription, scolarité et enseignement ; coefficients matière et évaluation strictement positifs ; barème strictement positif ; note positive ou nulle ; moyenne générale nulle ou positive. Les dates de fin facultatives sont acceptées. Le contrôle des fenêtres accepte une borne nulle.

**Ne sont pas garantis par le schéma actuel** : non-chevauchement, note ≤ barème, classe de la note = classe de l’évaluation, rattachement temporel, cohérence des années, vocabulaire des statuts hors rôle, unicité d’un bulletin officiel ou immutabilité. Les FK garantissent l’existence, pas ces règles transversales.

Il n’existe aucun index métier secondaire dédié aux FK. Par exemple, l’index UNIQUE de note commence par `id_scolarite`, pas par `id_evaluation`, et celui d’enseignement commence par `id_enseignant`, pas par `id_classe`. Prévoir les index utiles aux futures requêtes, sans dupliquer ceux déjà couverts par un préfixe d’index. PostgreSQL ne crée pas automatiquement d’index sur les colonnes référençantes d’une FK : [documentation PostgreSQL 17](https://www.postgresql.org/docs/17/ddl-constraints.html#DDL-CONSTRAINTS-FK).

Douze séquences d’identité ont été inspectées par `pg_sequences`, sans les avancer. Les valeurs dépassent parfois largement les nombres de lignes ; les trous d’identifiants sont normaux. Aucune renumérotation ou remise à zéro à prévoir. Une `last_value` absente ne suffit pas à conclure à un défaut de séquence.

### 2.4 Résultats des contrôles de données

| Contrôle réellement exécuté | Résultat | Interprétation |
|---|---:|---|
| Groupes classe–matière à coefficients contradictoires | 0 | Reprise vers PONDERER déterministe sur cet état |
| Coefficients matière ≤ 0 | 0 | Compatibles avec le contrôle actuel |
| Couples de périodes avec intersection, bornes inclusives | 3 | Les trois périodes ont les mêmes dates, libellés différents |
| Couples de scolarités d’un même élève avec intersection | 0 | Aucun conflit temporel détecté |
| Couples d’enseignements de même classe/matière avec intersection | 0 | Aucun cas de coenseignement/remplacement à résoudre actuellement |
| Notes dont la classe diffère de celle de l’évaluation | 0 | Chaîne note–scolarité–classe cohérente |
| Notes dont l’évaluation est hors dates de scolarité | 0 | Dates compatibles |
| Notes < 0 ou > barème | 0 | Pas de valeur à corriger |
| Évaluations hors dates de période | 0 | Dates compatibles |
| Évaluations hors dates d’enseignement | 0 | Dates compatibles |
| Périodes liées à plusieurs années via les évaluations | 0 | Année déductible sans conflit actuellement |
| Périodes sans année déductible via évaluations | 0 | Toutes ont au moins une évaluation exploitable |
| Scolarités sans inscription couvrante | 3 | Alerte métier ; aucune FK inscription–scolarité n’est attendue dans le MLD |
| Personnes à la fois enseignantes et responsables | 0 | Le cas recouvrant est absent des données, il faut le tester |
| Personnes sans spécialisation | 1 | Compatible avec l’héritage partiel, à conserver |
| Comptes ENSEIGNANT/RESPONSABLE sans profil correspondant | 0 | Le seul compte est ADMIN |
| Collisions futures bulletin par élève/période | 0 | Tables vides ; contrôle à rejouer avant migration |
| Collisions futures ligne par bulletin/matière | 0 | Tables vides ; contrôle à rejouer avant migration |
| Orphelins sur chacune des 22 FK | 0 | Vérifiés par anti-jointure, au-delà du catalogue |
| Fenêtres de saisie avec une seule borne | 0 | Les 3 fenêtres ont les deux bornes nulles |
| Notes hors fenêtre de saisie renseignée | 0 | Contrôle sans portée pratique ici, aucune fenêtre définie |
| Évaluations liées à un enseignement inactif | 0 | Ne préjuge pas de la politique historique sur `actif` |
| Notes liées à une scolarité autre qu’ACTIVE | 0 | Ne préjuge pas du calcul de bulletins historiques |

Les périodes **17, 20 et 28** couvrent toutes **2026-09-01 → 2026-12-31**, statut `OUVERTE`, sans fenêtre de saisie. Les paires en recouvrement sont `(17,20)`, `(17,28)`, `(20,28)`. Leurs années déduites par `evaluation → enseignement → classe` sont toutes `2026-2027`. Ce constat n’autorise ni leur fusion ni leur suppression : chaque période conserve son identifiant et ses évaluations. Une interdiction globale des chevauchements échouerait actuellement.

Pondérations à reprendre, sans donnée personnelle :

| id_classe | id_matiere | Enseignements | Valeurs distinctes | Coefficient |
|---:|---:|---:|---:|---:|
| 25 | 28 | 1 | 1 | 2.00 |
| 30 | 32 | 1 | 1 | 2.00 |
| 42 | 41 | 1 | 1 | 2.00 |

Attendu pour la reprise actuelle : **3 lignes PONDERER**, aucune création de produit cartésien classe × matière. Une classe sans enseignement ne doit pas recevoir artificiellement des coefficients.

### 2.5 Repères de préservation

Empreintes MD5 techniques de l’ensemble des lignes, sérialisées en JSONB et concaténées dans l’ordre de la PK (requête en annexe). Ce sont des repères de comparaison, pas une sauvegarde ni une preuve cryptographique. Ne pas les comparer aveuglément après ajout de colonnes : comparer alors la projection des colonnes d’origine.

| Table | Empreinte observée |
|---|---|
| enseignement | `4de5ebcb225fdc0ed72c2e3ca102971d` |
| scolarite | `92eb187449607483b4b4418d46e17223` |
| evaluation | `4c3b5d4fdc73778d1338c46618bb5554` |
| note | `77bbef78983d1211ad0ef928de6a3f38` |
| bulletin | `d41d8cd98f00b204e9800998ecf8427e` |
| ligne_bulletin | `d41d8cd98f00b204e9800998ecf8427e` |

Avant chaque future intervention : refaire les volumes, anomalies, projections et inventaire des FK ; les chiffres de ce rapport ne doivent jamais être codés comme conditions universelles d’une migration. Conserver également personnes/comptes, photos et stockage documentaire. Un PDF précédemment téléchargé hors de la base n’est pas inventorié par ces lectures.

## 3. Correspondance complète des 17 relations cibles

Les noms génériques Looping ne sont pas une obligation de renommer les clés physiques. Garder des noms explicites évite les confusions de rôle des personnes.

| Relation cible | Table actuelle / correspondance des attributs | Écart certain |
|---|---|---|
| PERSONNE | personne : id, nom, prénom, email, téléphone, adresse | Structure compatible ; mapping des profils à revoir |
| ENSEIGNANT | enseignant : `id_personne`, `numero_employe` | SQL compatible, cumul des profils à permettre en Java |
| RESPONSABLE | responsable : `id_personne`, `profession` | Même constat |
| ELEVE | eleve : `id_personne`, `matricule`, `date_naissance`, `photo` → `photo_url` | Nom technique compatible avec une référence photo, aucune conversion binaire nécessaire |
| COMPTE_UTILISATEUR | compte_utilisateur : `secret_authentification` → `mot_de_passe_hash`, autres attributs existants | Ajouter `habilitation_validation` ; conserver les secrets encodés |
| CLASSE | classe : `id_personne*` → `id_professeur_principal` facultatif vers enseignant, autres attributs existants | Compatible, ne pas élargir le référent à toute personne |
| MATIERE | matiere : `id_matiere`, `code`, `nom` | Compatible |
| PERIODE | periode : attributs existants | Ajouter `annee_scolaire` |
| INSCRIPTION | inscription : `id_personne` → `id_eleve`, dates, statut | Compatible ; table vide, couverture métier à décider |
| SCOLARITE | scolarite : `id_scolarité` → `id_scolarite`, `id_personne` → `id_eleve`, classe, dates, statut | Compatible ; préserver les 3 PK et leurs notes |
| ENSEIGNEMENT | enseignement : `id_personne` → `id_enseignant`, classe, matière, dates, actif | Extraire puis retirer `coefficient_matiere` |
| EVALUATION | evaluation : enseignement, période, libellé, date, type, coefficient, barème | Ajouter `motif_exception` |
| NOTE | note : scolarité, évaluation, valeur, commentaire, statut, saisie | Ajouter `motif_exception` |
| BULLETIN | bulletin : `id_personne` côté ÉTABLIR → **id_eleve** ; `id_personne_1` côté VALIDER → **id_validateur** vers personne ; `id_periode` existant | Remplacer dépendance à scolarité par élève ; version, officiel, gel et publication manquants |
| RESPONSABILITE_LEGALE | responsabilite_legale : personne côté RESPONSABLE → `id_responsable`, personne côté ELEVE → `id_eleve` | PK composée/FK déjà compatibles |
| PONDERER | absente ; source `enseignement(id_classe,id_matiere,coefficient_matiere)` | Nouvelle relation à PK `(id_classe,id_matiere)` |
| LIGNE_BULLETIN | ligne_bulletin : bulletin, future matière issue de l’enseignement, `texte` → `appreciation`, dates existantes | Clé cible `(id_bulletin,id_matiere)` ; moyenne et coefficient figés absents ; ancien statut absent du MLD |

## 4. Audit Spring Boot et frontend : impacts localisés

Racines : [Java](../../backend/src/main/java/fr/afpa/backend/), [tests Java](../../backend/src/test/java/fr/afpa/backend/), [frontend](../../frontend/src/). Stack déclarée : Java 21, Spring Boot 3.5.16, JPA/Hibernate, Flyway PostgreSQL, OpenPDF ; React/Vite. `ddl-auto: validate` doit être conservé : aucune génération de schéma Hibernate à substituer à Flyway.

### 4.1 Héritage et identité

[Personne](../../backend/src/main/java/fr/afpa/backend/entity/Personne.java) utilise `@Inheritance(JOINED)` ; Eleve, Enseignant et Responsable en héritent. Ce modèle Java classe une instance dans une hiérarchie de types ; il ne représente pas une personne simultanément instance des deux classes sœurs Enseignant et Responsable. La base, elle, n’interdit ni les profils multiples ni une personne seule. **Ne pas insérer des profils doubles sous le mapping actuel en supposant qu’Hibernate les interprétera correctement.**

Recommandation : Personne reste une entité autonome ; chaque profil devient une entité à PK partagée et association `@OneToOne @MapsId` vers Personne. Conserver les tables et tous les `id_personne`. L’utilisation de `@MapsId` avec une identité dérivée est documentée par [Hibernate](https://docs.hibernate.org/orm/6.6/userguide/html_single/#identifiers-derived). La refonte de cette hiérarchie en composition est une recommandation issue de l’analyse du code et de la règle de recouvrement.

Impacts : entités des trois profils ; leurs mappers qui appellent actuellement `new Eleve/Enseignant/Responsable` et accèdent directement aux coordonnées ; services de création/modification/suppression ; `PersonneRepository` et repositories des profils ; chargements `CompteUtilisateurRepository` ; DTO de profils ; `ElevePhotoService` ; tests et fixtures. Des accesseurs de délégation peuvent limiter les changements de contrat JSON, sans conserver l’héritage persistant.

[CompteUtilisateurService.validateRole](../../backend/src/main/java/fr/afpa/backend/service/CompteUtilisateurService.java) utilise `instanceof Enseignant/Responsable` : remplacer par l’existence du profil correspondant à l’identifiant. Créer un second profil doit réutiliser une personne existante choisie explicitement, sans fusion de personnes par nom ou email. Supprimer un profil ne doit plus supprimer sa Personne, son autre profil ou son compte par cascade JPA. Vérifier aussi les références de professeur principal et de validateur avant suppression.

Le rôle du compte est actuellement **unique** : enum, colonne, principal, JWT, formulaires. Le recouvrement des profils métier n’impose pas à lui seul plusieurs rôles d’accès simultanés. Si cet usage est requis, il faut une décision supplémentaire et un modèle d’autorisations distinct ; ne pas ajouter silencieusement une table de rôles absente du MLD.

### 4.2 Cartographie des modifications applicatives

| Zone/fichiers sous les racines ci-dessus | Modification attendue |
|---|---|
| `entity/Enseignement.java`, `dto/enseignement/*`, `mapper/EnseignementMapper.java`, `service/EnseignementService.java` | Déplacer l’écriture du coefficient vers PONDERER ; supprimer le champ persistant à terme ; transition API explicite |
| Nouveaux `Ponderer`, `PondererId`, repository, service, DTO, mapper, contrôleur | CRUD classe–matière avec unicité/positivité ; absence de pondération signalée, pas de valeur inventée |
| `repository/EnseignementRepository.java` | Requêtes par classe/matière et date ; garder les règles de propriété de l’enseignant |
| `service/BulletinCalculService.java` | Aujourd’hui une ligne et un coefficient par **enseignement** ; agréger les notes par **matière** pour tous les enseignements pertinents, puis pondérer une fois par matière |
| `entity/Bulletin.java`, `repository/BulletinRepository.java` | Élève à la place de scolarité, validateur Personne, version/états/gel/publication ; recherches par élève/période/version et verrouillage |
| `entity/LigneBulletin.java`, `repository/LigneBulletinRepository.java` | Matière, clé composée à terme, valeurs figées, appréciation ; remplacer `existsByEnseignement_IdEnseignement` |
| `dto/bulletin/BulletinDto.java`, `LigneBulletinDto.java` | Actuellement résultat calculé sans idBulletin/version/validation ; distinguer prévisualisation et bulletin persisté ; retirer l’enseignement comme identité de ligne |
| Futurs service de cycle de vie, mapper et DTO bulletin | Création de brouillon, validation, publication, révision, historique ; aucun service de ce cycle ni mapper bulletin n’existe actuellement |
| `service/BulletinPdfService.java` | Appelle actuellement `calculer` à chaque téléchargement ; exporter une version figée et servir le PDF archivé pour l’officiel |
| `controller/BulletinController.java` | Seulement deux GET `/bulletins/calcul/{idScolarite}/{idPeriode}` et `/pdf` ; nouveaux contrats par élève/période et bulletin/version à concevoir, sans détourner un GET en écriture |
| `entity/Periode.java`, `repository/PeriodeRepository.java`, `service/PeriodeService.java`, `dto/periode/*`, `mapper/PeriodeMapper.java`, `controller/PeriodeController.java` | Année scolaire, filtre, validation des dates/années et gestion du recouvrement décidé |
| `entity/Evaluation.java`, `entity/Note.java`, DTO/mappers/services/contrôleurs correspondants | Motifs d’exception ; contrôles temporels et règles de saisie décidées |
| `service/NoteService.java` | Vérifie déjà classe, doublon et valeur ≤ barème à la création et à la modification ; ne vérifie pas actuellement période ouverte, fenêtre de saisie ni dates de scolarité |
| `service/EvaluationService.java` | Vérifie les références et protège la suppression en présence de notes ; ne vérifie pas les dates période/enseignement ni les années ; modification possible des relations/barème malgré notes existantes |
| `service/ScolariteService.java`, `ScolariteRepository`, DTO/mappers/contrôleur | Détecte seulement le doublon exact élève/classe/début, pas le recouvrement ; modification de classe/élève possible malgré notes ; enrichir les garde-fous |
| `service/EnseignementService.java` | Doublon exact et protection suppression évaluation/ligne ; modification de classe/matière possible malgré évaluations ; après retrait des lignes, conserver la protection via évaluations |
| `service/ClasseService.java`, `MatiereService.java`, `EleveService.java`, `PeriodeService.java` | Revoir suppressions/modifications avec nouvelles références PONDERER/bulletins/lignes ; éviter de se reposer uniquement sur l’erreur FK ; protéger l’historique |
| `CompteUtilisateur`, DTO create/update/response, `CompteUtilisateurMapper`, service et contrôleur | Nouvelle habilitation, administrable seulement par une autorité permise ; DTO de réponse sans secret |
| `AuthenticationService`, DTO auth/register, `CompteUtilisateurPrincipal`, `JwtService`, `ActiveAccountJwtValidator`, `SecurityExpressions`, `SecurityConfig` | Intégrer l’habilitation et le périmètre de validation ; adapter la composition des profils ; conserver les comptes et règles d’accès existants |
| `FirstAdminBootstrap`, `CompteUtilisateurDetailsService` | Préserver le compte existant, la création d’une Personne sans profil et le chargement d’identité ; aucune habilitation déduite automatiquement du bootstrap |
| `Inscription*`, `ResponsabiliteLegale*` | Schéma/contrats essentiellement conservés ; mappings d’accès aux profils et tests à adapter ; couverture inscription à arbitrer |

Les repositories avec JPQL ou chemins dérivés de profil à vérifier comprennent **Eleve, Enseignement, Evaluation, Note, Scolarite, Classe, ResponsabiliteLegale et CompteUtilisateur**. Les identifiants de profils peuvent rester exposés comme `idPersonne`, mais toute expression d’accès aux attributs hérités doit être revue. Les 13 mappers existants ont été inventoriés ; tous les mappers de profils, d’enseignement, période, évaluation, note et compte sont touchés directement, les autres par leurs relations ou leurs gardes de suppression.

### 4.3 Calcul actuel, résultats figés et sécurité

Le calcul actuel normalise chaque note sur 20, pondère par coefficient d’évaluation, calcule avec 10 décimales et arrondit l’affichage à 2 en `HALF_UP`. Il exclut les matières sans moyenne du dénominateur général. Il filtre les enseignements par intersection de dates avec la période, mais n’effectue pas de contrôle explicite de cohérence année/période/scolarité. Il ne filtre pas les notes selon `statut_note`. Aucun de ces choix de calcul ne doit changer implicitement lors de la seule extraction de PONDERER.

Le découpage actuel par enseignement peut compter deux fois une matière lors d’un remplacement ou coenseignement. Le nouveau calcul doit agréger les **notes**, avec leurs coefficients d’évaluation, pour une matière ; faire la moyenne simple de moyennes d’enseignants produirait un autre résultat. Le passage au bulletin par élève doit également prendre en compte les scolarités pertinentes de la période et décider comment traiter un changement de classe et de coefficient.

La sécurité HTTP accorde aujourd’hui des GET à ADMIN/ENSEIGNANT/RESPONSABLE, l’écriture pédagogique à ADMIN/ENSEIGNANT et l’administration à ADMIN, avec refus par défaut. Les listes de plusieurs contrôleurs chargent les données puis filtrent via `SecurityExpressions` ; les lectures individuelles utilisent aussi `@PostAuthorize`. Le bulletin calculé utilise `peutConsulterScolariteOuAbsente`. Après rattachement à l’élève, il faut une autorisation sur **le bulletin et son élève/période**, pas seulement sur un identifiant de scolarité fourni par le client. Les filtres liés aux enseignements/scolarités n’intègrent pas tous une portée temporelle.

Le JWT porte une liste contenant un seul rôle, ainsi que les identifiants compte/personne. `ActiveAccountJwtValidator` relit le compte et vérifie actif, sujet, personne et rôle. Il n’y a aucune habilitation de validation. Recommandation : vérifier **en base, au moment de l’action**, compte actif + habilitation + périmètre ; déduire `id_validateur` du compte authentifié. Une révocation doit agir sans attendre l’expiration du JWT. Un éventuel claim de capacité sert à l’interface, pas d’autorisation définitive. Prévoir la cohérence transactionnelle entre révocation et validation concurrentes.

Le rôle ADMIN ne doit pas devenir automatiquement habilité sans décision explicite. Le modèle autorise un validateur Personne qui n’est pas enseignant ; la chaîne de filtres ne doit donc pas imposer ce profil sur le futur endpoint. Le contrôle de périmètre et le droit d’accorder l’habilitation restent à définir.

### 4.4 Frontend

| Fichier | Changement associé |
|---|---|
| [AdministrationPages.jsx](../../frontend/src/pages/AdministrationPages.jsx) | Retirer l’édition du coefficient dans Enseignements après transition ; page/grille PONDERER ; année dans Périodes ; motifs dans Évaluations ; rattachement d’un profil à une Personne existante |
| [BulletinsPage.jsx](../../frontend/src/pages/BulletinsPage.jsx) | Sélection élève/année/période ; prévisualisation, brouillon, versions, validation/publication/révision ; valeurs figées ; clés React `idMatiere` au lieu de `idEnseignement` |
| [bulletinsApi.js](../../frontend/src/api/bulletinsApi.js) | Actuellement calcul/PDF par scolarité ; ajouter lecture persistée et actions de cycle de vie ; PDF par version |
| [ComptesUtilisateursPage.jsx](../../frontend/src/pages/ComptesUtilisateursPage.jsx) | Champ d’habilitation ; personnes multi-profils ; dédoublonner par idPersonne la concaténation enseignants/responsables et représenter les profils disponibles |
| [NotesPage.jsx](../../frontend/src/pages/NotesPage.jsx) | Motif d’exception ; cohérence temporelle et états décidés ; préserver note/évaluation/scolarité comme références métier |
| `api/schoolApi.js`, `adminApi.js`, `components/school/schoolData.js`, `ResourcePage.jsx` | Nouvelle ressource pondération, labels de période avec année, adaptation des formulaires et appels |
| `context/AuthContext.jsx`, `authContextCore.js`, stockage/auth API, `ProtectedRoute.jsx`, `App.jsx`, `DashboardPage.jsx`, `AppHeader.jsx` | Afficher les capacités et actions permises ; nouvelles routes/navigation ; ne pas confondre profil, rôle et habilitation |
| `pages/ElevesPage.jsx`, `ResponsabilitesLegalesPage.jsx` | Vérifier contrat photo/identité et associations après refonte des profils |

Les boutons masqués ne remplacent jamais les autorisations backend. Conserver les contrats existants autant que possible pendant les ajouts ; retirer un champ/API seulement dans un lot de bascule coordonné.

## 5. Deltas physiques proposés : ajouts, modifications, retraits

Les noms d’attributs et relations proviennent du MLD ; **types, tailles, valeurs par défaut et contraintes supplémentaires ci-dessous sont des propositions techniques**, car un PNG ne spécifie pas un MPD complet. `TEXT` pour textes/motifs, `NUMERIC(5,2)` pour rester compatible avec les précisions actuelles, `TIMESTAMPTZ` pour les événements. Le choix exact des statuts/politiques reste à valider avant de coder des CHECK fermés.

### 5.1 Ajouts

| Table | Colonnes à ajouter | Contrat cible proposé |
|---|---|---|
| ponderer (nouvelle) | `id_classe BIGINT`, `id_matiere BIGINT`, `coefficient_matiere NUMERIC(5,2)` | Tous NOT NULL ; PK composée ; FK classe/matière NO ACTION ; coefficient > 0 ; pas de coefficient métier implicite |
| compte_utilisateur | `habilitation_validation BOOLEAN` | NOT NULL DEFAULT false, y compris comptes existants ; aucune attribution en masse |
| periode | `annee_scolaire VARCHAR(20)` | Nullable lors de l’extension ; NOT NULL après reprise vérifiée ; même format métier que classe |
| evaluation | `motif_exception TEXT` | Nullable ; pas de motif historique inventé |
| note | `motif_exception TEXT` | Nullable ; même principe |
| bulletin | `id_eleve BIGINT` | FK eleve(id_personne), d’abord nullable puis NOT NULL |
| bulletin | `numero_version INTEGER` | D’abord nullable ; cible NOT NULL et > 0 ; reprise déterministe des éventuels anciens bulletins |
| bulletin | `officiel_courant BOOLEAN` | NOT NULL DEFAULT false ; au plus un officiel courant par élève/période |
| bulletin | `moyenne_generale_arretee NUMERIC(5,2)` | Nullable sans moyenne ; contrôle bornes à définir, pas de 0 pour « non calculable » |
| bulletin | `date_publication TIMESTAMPTZ` | Nullable avant publication |
| bulletin | `motif_revision TEXT` | Nullable pour version initiale ; exigence conditionnelle à décider |
| bulletin | `politique_coefficients TEXT` | Nullable en extension ; obligatoire pour une validation selon règle à définir ; code de politique explicite et interprétable |
| bulletin | `reference_pdf_officiel TEXT` | Nullable avant archivage ; référence stable par version, sans URL publique libre |
| ligne_bulletin | `id_matiere BIGINT` | FK matiere, d’abord nullable puis NOT NULL |
| ligne_bulletin | `moyenne_arretee NUMERIC(5,2)` | Nullable pour matière sans résultat, ne pas fabriquer une moyenne |
| ligne_bulletin | `coefficient_utilise NUMERIC(5,2)` | Nullable en extension ; cible > 0 et renseigné pour ligne figée selon politique |
| ligne_bulletin | `appreciation TEXT` | Reprendre `texte`, sans perte de contenu |

Un champ technique de verrouillage optimiste `lock_version BIGINT` avec `@Version` peut être ajouté dans le lot cycle de vie si ce mécanisme est retenu. Il est **distinct** de `numero_version`, qui est une version métier. Ce champ n’est pas une exigence du MLD.

### 5.2 Modifications et suppressions différées

| Objet | Transformation | Condition avant retrait/bascule |
|---|---|---|
| bulletin.id_validateur | Garder nom/type BIGINT nullable ; FK vers **personne** au lieu d’enseignant ; Java `Personne` | Ajouter/valider la nouvelle FK avant retrait de l’ancienne ; conserver IDs ; recommander NO ACTION pour préserver l’auteur historique plutôt que SET NULL |
| bulletin.id_scolarite | Migrer `id_eleve` depuis scolarite ; lever NOT NULL lors de la bascule vers les nouvelles écritures ; supprimer ensuite FK/colonne | Tous les lecteurs et contrôles d’accès basculés, provenance conservée si nécessaire |
| bulletin.moyenne_generale | Copier la valeur persistée vers `moyenne_generale_arretee` ; supprimer plus tard l’ancienne colonne/CHECK | Aucune réévaluation silencieuse des anciens résultats |
| bulletin.date_generation | Absent du MLD ; garder temporairement puis retirer, ou conserver comme métadonnée technique documentée | Décision explicite sur sa valeur historique ; ne pas la convertir en date de publication |
| uq_bulletin_scolarite_periode | Remplacer par UNIQUE `(id_eleve,id_periode,numero_version)` | Clés nouvelles complètes, collisions traitées et ancienne écriture arrêtée |
| ligne_bulletin.id_enseignement | Dériver `id_matiere`, lever NOT NULL à la bascule, puis supprimer FK/colonne | Contrôle de collision et retrait des usages Java/API |
| ligne_bulletin.id_ligne_bulletin | Retirer à terme pour la PK MLD `(id_bulletin,id_matiere)` | Conserver en transition ; aucune FK entrante observée ; vérifier de nouveau avant suppression |
| uq_ligne_bulletin_enseignement | Remplacer par UNIQUE puis PK `(id_bulletin,id_matiere)` | Aucune matière en double dans une version |
| ligne_bulletin.texte | Copier vers `appreciation`, puis retirer | Égalité de contenu vérifiée ; renommage direct seulement après arrêt des anciens lecteurs |
| ligne_bulletin.statut | Absent du MLD ; lever NOT NULL lors de la bascule si le nouveau code ne le renseigne plus, puis retirer après passage au statut porté par bulletin | Si des lignes apparaissent avant migration, archiver/interpréter les statuts sans perte |
| enseignement.coefficient_matiere | Extraire vers ponderer, déprécier, puis supprimer colonne et `ck_enseignement_coefficient` | Couverture exhaustive et égalité ; aucun ancien écrivain ; code de calcul/API basculé |

**Aucune suppression d’autre colonne existante n’est nécessaire pour ce MLD.** Conserver notamment `mot_de_passe_hash`, `photo_url`, `id_professeur_principal`, `id_enseignant`, `id_eleve`, `id_responsable`, et les identifiants de scolarité/note/évaluation. Les renommer selon les suffixes génériques de Looping n’apporte pas de changement métier.

### 5.3 Contraintes et index à prévoir

- PONDERER : PK `(id_classe,id_matiere)`, deux FK et CHECK coefficient > 0. Ajouter un index commençant par `id_matiere` si utile aux recherches/suppressions.
- Bulletin : FK élève/période/personne ; UNIQUE `(id_eleve,id_periode,numero_version)` ; index UNIQUE partiel `(id_eleve,id_periode) WHERE officiel_courant` ; CHECK version > 0. Compléter les cohérences statut/date/validateur/politique après décision métier.
- Ligne : PK finale `(id_bulletin,id_matiere)`, FK matière ; CHECK coefficient positif et moyenne selon échelle retenue ; conserver les dates de saisie/modification. Les brouillons peuvent nécessiter des champs nuls, contrairement aux versions validées.
- Index candidats pour le calcul et les relations : `enseignement(id_classe,id_matiere)`, `evaluation(id_enseignement,id_periode)`, `evaluation(id_periode)`, `note(id_evaluation)`, `scolarite(id_eleve,date_debut)`, `scolarite(id_classe)`, `responsabilite_legale(id_eleve)`, puis index des nouvelles références matière/validateur si justifiés. Les index actuels couvrent déjà certaines recherches par compte/personne et note/scolarité.
- Les contrôles croisés (barème, classe, année, dates) doivent être atomiques dans les services et protégés sur **toutes** les mutations des objets parents. Si plusieurs écrivains accèdent à la base, envisager des triggers ciblés ou une refonte des clés pour les garantir aussi côté SQL. Un CHECK ne peut pas vérifier correctement une autre table : [PostgreSQL 17, CHECK](https://www.postgresql.org/docs/17/ddl-constraints.html#DDL-CONSTRAINTS-CHECK-CONSTRAINTS).
- Ne pas ajouter de contrainte globale EXCLUDE sur les périodes avant décision et traitement des recouvrements. Une exclusion de scolarités par élève est envisageable si le métier interdit toute simultanéité, avec `daterange`, bornes documentées et éventuellement `btree_gist`. Elle doit aussi résister à deux transactions concurrentes.

## 6. Reprise des données sans perte

### PONDERER

1. Refaire le GROUP BY sur **tous** les enseignements, actifs ou historiques. Le coefficient appartient à la classe, qui porte déjà l’année scolaire : ne pas regrouper par nom de classe ou code matière en perdant les identifiants.
2. Échouer explicitement si un groupe a plusieurs coefficients ou une valeur invalide ; ne choisir ni moyenne, ni maximum, ni « dernier enseignant » par défaut.
3. Insérer une ligne par groupe après vérification de l’unicité de valeur. Un `MIN` technique n’est acceptable que si `COUNT(DISTINCT)=1` est garanti auparavant. Actuellement les trois groupes valent 2.00.
4. Vérifier le nombre de groupes, l’anti-jointure enseignement→ponderer et l’égalité de chaque coefficient. Ne toucher à aucun id_enseignement, aucune évaluation ni note.
5. Entre remplissage et bascule, une seule source d’écriture doit faire autorité. Option recommandée pour ce petit volume : courte suspension des écritures pédagogiques, reprise finale vérifiée, déploiement coordonné. Une double écriture applicative seule n’est pas sûre si d’anciens processus écrivent encore. Après bascule, rejeter une modification de coefficient via une ancienne API ou la traduire explicitement sous les mêmes garanties ; ne pas laisser deux valeurs diverger.

### PERIODE

Reprendre l’année depuis les classes des enseignements des évaluations **et, s’ils existent au moment de la reprise, des scolarités des bulletins historiques**. N’assigner automatiquement que si toutes les sources concordent sur une année unique. Actuellement la voie évaluations suffit pour les 3 périodes et produit `2026-2027`. Pour une période sans source ou avec plusieurs années, arrêter le lot concerné et demander une résolution explicite, sans deviner à partir du seul mois de début. Rejouer les contrôles après remplissage avant NOT NULL.

Conserver les trois périodes malgré les dates identiques. Si une consolidation est décidée ultérieurement, il faudra une migration distincte avec table de correspondance, contrôle des effets sur notes/bulletins, et validation métier ; ce rapport ne l’autorise pas.

### BULLETIN et LIGNE_BULLETIN

Les deux tables sont vides **à la date de l’audit uniquement**. Rejouer le contrôle au déploiement. Recommandation pour les premières migrations de bulletin : une précondition explicite d’absence d’historique permet une bascule simple sur une base vide ; si elle n’est plus vraie, échouer sans supprimer ni recalculer et traiter la reprise générale ci-dessous dans un lot adapté.

Pour des données apparues entre-temps : dériver l’élève du bulletin via scolarité, conserver l’id_bulletin et l’auteur existant (tout enseignant est déjà une personne). Préserver moyennes et appréciations persistées. En cas de plusieurs bulletins du même élève/période, attribuer des versions après examen de l’historique, avec ordre déterministe et traçabilité ; ne pas affirmer qu’un tri par identifiant prouve la chronologie métier. Ne jamais déclarer un officiel par défaut.

Pour les lignes, dériver la matière de l’enseignement. Plusieurs lignes d’enseignements d’une même matière peuvent entrer en collision avec la clé cible : conserver leur provenance et résoudre le contenu explicitement. Ne pas moyenner des moyennes ni concaténer aveuglément des appréciations.

**L’ancien schéma ne stocke ni moyenne arrêtée de ligne ni coefficient utilisé.** Une reconstitution depuis les notes et coefficients courants ne prouve pas le contenu historique d’un bulletin validé. Si de tels bulletins existent, conserver les anciens enregistrements et PDF/preuves disponibles, qualifier l’absence d’instantané, et bloquer la conversion en version officielle tant qu’une politique de reprise n’est pas décidée. Actuellement ce risque de reprise est évité par les tables vides.

À l’avenir : calculer un instantané cohérent, enregistrer les lignes et totaux, la politique appliquée, l’identité du validateur et les dates. Une modification ultérieure des notes, pondérations ou enseignements ne modifie jamais une version validée. Une révision crée une nouvelle version. Les libellés/identités/classes affichés peuvent aussi changer : conserver le PDF officiel immuable et décider si un instantané additionnel de ces informations est nécessaire, car le MLD ne le contient pas explicitement.

### Notes, évaluations, scolarités, identités et comptes

Conserver intégralement les PK/FK, valeurs, coefficients d’évaluation, barèmes, commentaires, statuts et dates de saisie. Les motifs d’exception ajoutés restent NULL pour l’historique ; les lectures n’établissent pas les raisons d’une ancienne exception. Ne pas transformer `SAISIE` en `VALIDEE` et ne pas remplir une inscription à partir d’une scolarité sans justification métier.

Les profils en composition gardent les mêmes tables et identifiants : aucune copie/duplication d’identité, aucune fusion automatique. Préserver le compte ADMIN actif et le hash existant, la normalisation des emails et la protection du dernier administrateur. `habilitation_validation=false` est le seul défaut proposé ; l’attribution ultérieure est une action explicite et distincte.

## 7. Règles certaines et décisions métier ouvertes

| Sujet | Établi par le modèle/la mission | Décision restant nécessaire avant implémentation dépendante |
|---|---|---|
| Héritage | Partiel et recouvrant ; enseignant ET responsable possible | Parcours d’ajout/suppression d’un profil existant ; accès simultané aux deux fonctions avec un compte à rôle unique |
| Coefficient | Un coefficient pour un couple classe–matière | Modification en cours d’année/période, historique des coefficients et résolution d’un conflit futur |
| Bulletin | Un élève, une période, des versions, un validateur Personne facultatif | Statuts, transitions, nombre de brouillons, moment où une version devient « officiel courant » |
| Validation | Habilitation spécifique du compte | Qui peut l’accorder ; périmètre classe/établissement ; cumul ADMIN ; validation de son propre travail ; besoin éventuel de séparation des tâches |
| Résultats figés | Moyennes/coefficient de ligne et moyenne générale arrêtés | Gel à la création, validation ou publication ; politique d’arrondi et de notes éligibles ; coefficient manquant ; matières sans notes |
| Mobilité d’un élève | Notes liées à une scolarité, bulletin lié à l’élève | Scolarités à inclure lors d’un changement de classe ; coefficient retenu pour une matière présente dans plusieurs classes ; classe affichée sur le PDF |
| politique_coefficients | Attribut présent dans BULLETIN | Valeurs autorisées, version de l’algorithme, représentation exacte et reproductibilité de la politique |
| Révision | Attribut motif_revision et numéro de version | Motif obligatoire dès v2 ? qui peut réviser, nouveau circuit de validation, conservation des anciens officiels |
| Publication/PDF | date_publication et reference_pdf_officiel présentes | Stockage, archivage, intégrité, échec de génération, permissions de téléchargement et durée de conservation |
| Périodes | Année scolaire requise | Recouvrement autorisé (types de périodes parallèles ?) ou anomalies à corriger ; bornes inclusives ; format/calendrier scolaire |
| Scolarités/inscriptions | Schémas séparés et scolarités existantes à préserver | Simultanéité interdite ou non ; obligation d’une inscription couvrante ; gestion des trois scolarités sans inscription |
| Exceptions | motif_exception sur note et évaluation | Cas concernés : date hors période, période close, saisie hors fenêtre ; autorité de dérogation ; motif ne doit pas autoriser toute incohérence structurelle |
| Statut des notes | Deux SAISIE et une VALIDEE existantes | Statuts pris en compte au calcul, absence/dispense et éventuelle valeur nullable ; aucun changement de nullabilité requis par le seul MLD |
| Effacement historique | Versions et validateur à conserver | Politique d’archivage/anonymisation ; éviter la perte d’auteur par SET NULL et la suppression en cascade des lignes officielles |

Ces décisions ne bloquent pas les ajouts de colonnes facultatives, les contrôles reproductibles, la reprise non ambiguë de PONDERER ni les tests isolés. Elles bloquent les migrations qui figeraient une interprétation non convenue.

## 8. Ordre recommandé des migrations et des déploiements

Les numéros suivants constituent une **feuille de route proposée**, pas des fichiers créés. Ne livrer que les migrations du lot prêt : Flyway au démarrage peut appliquer toutes les versions présentes, même si le code n’est pas encore basculé. V1 reste immuable. Chaque étape doit réussir sur une base neuve V1→cible et sur une copie de la base actuelle.

| Migration proposée | Contenu borné | Vérification de sortie / coordination |
|---|---|---|
| **V2__create_ponderer** | Créer PONDERER et reprendre les groupes non ambigus, avec arrêt sur contradiction | 3 groupes attendus sur l’instantané audité, zéro différence ; ancien champ conservé et ancienne application toujours compatible |
| **V3__add_periode_annee_scolaire** | Ajouter l’année nullable, sans règle de non-recouvrement | Application actuelle encore compatible ; aucune perte d’évaluation |
| **V4__backfill_periode_annee_scolaire** | Remplir les années depuis les relations concordantes ; garder chaque période | Ici 3 périodes en 2026-2027 ; rejeter ambiguïtés ; déployer le code qui renseigne l’année pour les nouvelles écritures |
| **V5__add_validation_habilitation** | Ajouter booléen NOT NULL DEFAULT false | Compte conservé ; aucune nouvelle permission effective ; prévoir UI/service d’administration avant usage |
| **V6__add_exception_motifs** | Ajouter les deux TEXT nullable | Projection des anciennes colonnes note/évaluation inchangée ; règles de dérogation implémentées séparément |
| **V7__expand_bulletin_versioning** | Ajouter les attributs du bulletin, id_eleve, nouvelle FK du validateur vers Personne ; garder l’ancienne FK et les anciens champs | Ancienne application compatible ; éventuel remplissage seulement selon préconditions décidées ; pas de faux officiels |
| **V8__expand_ligne_bulletin_snapshots** | Ajouter matière, moyenne, coefficient et appréciation, sans retrait de champs | Reprise de la matière/du texte si applicable, contrôle des collisions ; aucun résultat historique inventé |
| **V9__enable_bulletin_versions** | Compléter/valider clés nouvelles, contraintes d’unicité de version et officiel courant, UNIQUE des lignes `(id_bulletin,id_matiere)` ; retirer FK validateur→enseignant ; lever NOT NULL des deux anciennes dépendances scolarité/enseignement et de l’ancien statut de ligne ; retirer l’ancienne unicité bulletin/scolarité/période | **Bascule coordonnée** vers le service de versions ; précondition tables vides ou reprise historique approuvée ; anciennes écritures bulletin désactivées, ancien code non redéployable pour ce parcours |
| **V10__enforce_periode_year** | NOT NULL année, après contrôle complet | Tous les écrivains fournissent l’année ; pas d’EXCLUDE ajouté par opportunité |
| **V11__contract_enseignement_coefficient** | Reprise finale vérifiée si nécessaire puis retrait coefficient ancien/CHECK | Code et frontend PONDERER en service ; aucune divergence ni ancien écrivain ; suspension courte des écritures pendant bascule finale si nécessaire |
| **V12__contract_bulletin_legacy** | Retirer colonnes scolarité/moyenne ancienne et dates obsolètes selon décision, anciennes contraintes restantes | Nouveaux lecteurs/exports/autorisations opérationnels ; provenance archivée si nécessaire ; zéro référence au champ retiré |
| **V13__contract_ligne_bulletin_legacy** | PK composée, retrait de l’id technique si retenu, enseignement, texte et statut anciens | Mappings Java/DTO déjà prêts ; zéro collision ; usages du repository ancien retirés |

Pour V9, un index UNIQUE des versions ne suffit pas à lui seul à rendre l’ancien code sûr ; l’ancienne unicité empêche plusieurs versions pour une même scolarité/période tant qu’elle est présente. De même, de nouvelles lignes sans enseignement et de nouveaux bulletins sans scolarité sont impossibles tant que les anciens NOT NULL demeurent. L’ancien statut de ligne obligatoire empêcherait également une insertion par un nouveau mapping qui l’omet : le rendre nullable à cette bascule. Ces modifications doivent précéder l’ouverture des nouveaux endpoints, sans être amalgamées à un lot de suppression définitive. Coordonner également la PK composée et son mapping Java dans V13 ; jusque-là, garder une identité technique transitoire et une contrainte UNIQUE sur le couple cible.

La refonte **Personne/profils en composition** est un lot Java autonome à placer avant l’activation du cumul des profils et avant les tests métier de validateur multi-profils. Elle ne nécessite normalement **aucun DDL** : les PK/FK partagées existent déjà. La lancer avec des tests JPA sur PostgreSQL isolé, puis adapter mappers, services, sécurité et UI sans changer les identifiants. Elle peut être développée après le premier lot PONDERER.

Les index secondaires et éventuelles contraintes temporelles sont à ajouter dans de petites versions ultérieures, après choix des règles et mesure des requêtes. Ne pas regrouper tous les index suggérés avec une migration de données.

### Discipline de chaque lot

1. Sauvegarde restaurable et test de restauration **sur un environnement distinct**, à préparer lors de l’implémentation ; aucune sauvegarde/restauration n’a été lancée par cet audit.
2. Rejouer les lectures de contrôle juste avant la migration ; empêcher la divergence pendant les reprises et bascules, par arrêt des écrivains concernés ou protocole atomique démontré.
3. Ajouter, remplir, vérifier, puis contraindre ; supprimer uniquement dans une version ultérieure après bascule du code. Les FK/CHECK peuvent être ajoutés `NOT VALID` puis validés séparément ; cela ne s’applique pas à une PK/UNIQUE et ne garantit pas un DDL sans verrou : [ALTER TABLE PostgreSQL 17](https://www.postgresql.org/docs/17/sql-altertable.html).
4. Transaction de migration atomique avec délais de verrouillage, préconditions et exceptions explicites. Ne pas masquer un conflit par `ON CONFLICT DO NOTHING` ou des `IF NOT EXISTS` sans vérification du résultat attendu.
5. Comparer volumes, clés, relations et valeurs originales ; documenter les seules modifications attendues. Avec de nouveaux champs, comparer la projection d’origine, pas `SELECT *`.
6. Tester `ddl-auto=validate`, démarrage, contrats API et autorisations sur base isolée. Ne jamais lancer ce démarrage sur l’existante pendant la phase d’audit.
7. En cas d’échec : rollback de la transaction échouée ; après un lot additif réussi, retour possible à l’ancienne application si ses invariants restent satisfaits. Après bascule destructive ou création de versions, privilégier une correction en avant ; aucune promesse de retour arrière sans restauration et gestion des écritures survenues depuis la sauvegarde.

### Cohérence du cycle de vie et concurrence

Un instantané ne doit pas mélanger des notes d’instants différents : définir une transaction et un protocole de gel/verrouillage des données sources (ou une révision identifiable) pour calcul/validation. Une transaction cohérente de lecture ne résout pas à elle seule les règles de validation concurrente.

Pour numéroter les versions, ne pas utiliser `MAX(numero_version)+1` sans sérialisation. Verrouiller par exemple la ligne Eleve, commune à toutes les créations pour cet élève, et conserver l’unicité élève/période/version comme dernier rempart. Le même verrou doit couvrir le remplacement de l’officiel courant ; l’index partiel empêchera deux officiels même si le service comporte une erreur. Rejouer les règles d’état dans la transaction, avec gestion explicite des conflits HTTP 409.

La version validée et ses lignes deviennent immuables, à l’exception de métadonnées de publication prévues par le cycle. Encadrer aussi la suppression du bulletin et la cascade de ses lignes. Les nouvelles notes corrigées alimentent une nouvelle version. Pour une garantie SQL face à d’autres écrivains, prévoir des triggers de protection ciblés et des tests des mises à jour/suppressions directes sur base isolée.

Le stockage PDF externe n’est pas transactionnel avec PostgreSQL : produire un objet durable par version, vérifier son existence/intégrité, puis publier la référence dans une transaction contrôlée ; prévoir reprise des erreurs, idempotence et nettoyage différé des objets sans référence. Un échec ne doit pas laisser une version annoncée officielle sans document accessible. Ce mécanisme est à concevoir dans le lot publication, sans ajouter ici une infrastructure non décidée.

## 9. Tests existants et plan de vérification

Inventaire statique : **40 fichiers de tests Java, 325 annotations `@Test`** (18 fichiers service/175 tests, 15 contrôleur/117, 6 sécurité/32, un test de contexte). Frontend : un fichier Vitest de rôles et **12 fichiers Playwright**. Ces nombres ne prouvent pas que la suite passe : aucun test n’a été exécuté pendant cet audit.

Tests existants particulièrement utiles : `BulletinCalculServiceTest` (pondérations multi-matières, absence de notes, arrondis HALF_UP, enseignements hors période), `BulletinPdfServiceTest`, `BulletinControllerTest`, `NoteServiceTest` (classe/barème/doublons), `EnseignementServiceTest`, `EvaluationServiceTest`, `PeriodeServiceTest`, `ScolariteServiceTest`, `CompteUtilisateurServiceTest`, tests JWT/compte actif et sécurité HTTP.

**Risque d’exécution locale identifié** : `BackendApplicationTests`, `SecurityIntegrationTest` et `AuthorizationHttpTest` utilisent `@SpringBootTest`. Il n’y a pas de ressources/profil de test isolant la datasource et Flyway dans `src/test/resources`, ni de Testcontainers/DataJpaTest observé. Même avec des services mockés, charger le contexte peut initialiser datasource, Flyway et bootstrap. Les Playwright créent/modifient/suppriment via l’API locale ; `scripts/test-e2e.sh` démarre Compose et potentiellement le backend. Ces suites ne doivent pas être lancées contre l’existante pour cet audit. La CI dispose de services PostgreSQL dédiés à ses jobs : conserver cette isolation et la rendre explicite dans les nouveaux tests locaux.

| Lot | Tests à créer ou adapter | Critères mesurables |
|---|---|---|
| Audit et Flyway | PostgreSQL 17 jetable, fixtures anonymes V1 ; chaîne V1→chaque lot ; contrôle checksum V1 ; reprise interrompue | Une base neuve et une copie V1 passent ; une contradiction échoue atomiquement ; aucune note/PK/FK perdue |
| PONDERER | Repository SQL, service, API, DTO ; adapter EnseignementService/Controller et calcul ; E2E dédié | Unicité du couple, coefficient > 0, 3 valeurs reprises, aucun ancien champ devenu source divergente |
| Calcul par matière | Étendre BulletinCalculServiceTest | Deux enseignants/remplacement même matière : une ligne, coefficients d’évaluation respectés, pas de moyenne de moyennes ; cas sans notes, barèmes variés, arrondis existants |
| Année et temporalité | Periode/Evaluation/Note/Scolarite tests, mutations des parents | Année unique/absente/contradictoire ; changement de classe/barème/période refusé s’il invalide des notes ; bornes et dates de fin nulles ; fenêtre et dérogation selon décision |
| Profils | Tests JPA avec flush, clear et relecture par les trois repositories | Personne seule ; enseignant+responsable **même PK** ; ajout/retrait d’un profil ; autre profil/compte/notes conservés ; DTO/photo inchangés |
| Habilitation | CompteUtilisateurService/Controller, principal/JWT/ActiveAccountJwtValidator, SecurityExpressions et sécurité HTTP | Actif habilité dans périmètre autorisé ; non habilité/inactif/hors périmètre refusé ; révocation immédiate ; identité validateur issue du serveur ; aucune auto-attribution via register |
| Bulletins persistés | Nouveaux service/repositories/DTO/contrôleur et tests SQL | Élève/période/version uniques ; brouillon→validation→publication→révision selon règles ; cohérence des dates et champs ; aucun doublon officiel |
| Immutabilité | Services et SQL sur instance isolée | Modifier notes/coefficient/nom matière après validation ne change pas les résultats arrêtés/PDF officiel ; révision distincte ; suppression officielle refusée |
| Concurrence | Deux transactions réellement concurrentes sur PostgreSQL | Créations simultanées sérialisées ; publication double impossible ; révocation concurrente prise en compte ; aucun brouillon partiellement figé |
| PDF | Adapter BulletinPdfServiceTest et contrôleur, tests stockage simulé | Export de la bonne version ; ancienne version stable ; échec/reprise d’archivage ; accès au PDF d’autrui refusé |
| Frontend | Vitest + nouveaux scénarios dans admin-enseignements/periodes/evaluations/notes/bulletins et roles-access | Formulaires PONDERER/année/motif, affichage capacité, versions et 409, masquage fonctionnel, refus backend, identité de ligne par matière |

Les tests de migration doivent comparer **les valeurs et relations**, pas seulement des nombres de lignes. Conserver notamment valeurs et dates des notes, coefficients/barèmes des évaluations et liens aux scolarités. Ajouter des fixtures avec données non vides de bulletin pour vérifier l’arrêt explicite des migrations supposant l’absence d’historique.

## 10. Risques et réponses

| Risque | Niveau / constat | Réponse |
|---|---|---|
| Perte des notes ou de leurs relations | Élevé, données présentes | Aucun remplacement des tables pédagogiques ; comparaison des projections/PK/FK ; sauvegarde restaurée sur copie avant intervention |
| Mauvaise lecture du XT temporaire | Élevé | Règle partielle/recouvrante explicite ; aucun CHECK de totalité/exclusion ; mapping de profils en composition |
| Coefficient arbitraire ou divergent | Élevé en concurrence, pas de contradiction actuelle | Reprise univoque, blocage des conflits, source unique d’écriture, contrôle final avant retrait |
| Recouvrement des périodes | Constat réel : 3 paires | Préserver IDs et évaluations ; décider avant d’interdire ou consolider |
| Scolarité sans inscription | Constat réel : 3 | Signaler ; pas de fausse inscription ; exigence de couverture dans un lot distinct si décidée |
| Validation accordée par simple rôle | Élevé fonctionnel | Booléen false par défaut, autorité d’attribution définie, contrôle serveur courant, pas d’id_validateur client |
| Historique recalculé | Élevé pour un futur officiel | Valeurs arrêtées et PDF immuable ; révision ; bloquer toute reprise historique sans preuve suffisante |
| Transfert de classe pendant période | Règle non définie | Décider sélection des scolarités et politique des coefficients avant bulletin par élève |
| Double version/double officiel | Élevé avec actions simultanées | Verrou partagé, UNIQUE de version, index partiel officiel, tests de concurrence |
| Ancien code après DDL incompatible | Élevé pendant bascule | Lots additifs séparés, déploiement coordonné, anciennes écritures bloquées, suppressions tardives |
| Suppression d’identité/auteur par cascade | Élevé avec profils multiples/historique | Supprimer uniquement le profil voulu ; FK validateur Personne restrictive ; protéger versions/lignes validées |
| Tests ou bootstrap sur la base existante | Élevé avec configuration locale actuelle | Aucune exécution ici ; datasource/instance de test dédiée avec garde contre URL existante |
| Verrouillage DDL | Faible volume actuel, risque opérationnel réel | Petites transactions, délais, essai sur copie ; planifier les contraintes ; index concurrents seulement si le volume le justifie et dans une migration adaptée |
| Fuite de données dans rapport/logs | Évitée dans ce livrable | Agrégats et identifiants structurels ; ne pas journaliser secrets ou contenu personnel lors des reprises |

## Annexe A — Rejouer l’inventaire strictement en lecture seule

Les blocs suivants ne sont pas des migrations. Ils documentent les lectures utilisées et permettent leur répétition autonome. Exécuter depuis la racine du dépôt, avec l’accès Docker approprié. Le compte et la base sont ceux de Compose ; aucun secret n’est nécessaire dans la sortie.

```bash
docker compose exec -T \
  -e PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=15000 -c lock_timeout=3000' \
  postgres psql -X -P pager=off -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves <<'SQL'
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT current_database(), current_setting('server_version'),
       current_setting('transaction_read_only'), current_timestamp;

SELECT table_schema,table_name FROM information_schema.tables
WHERE table_schema NOT IN ('pg_catalog','information_schema') ORDER BY 1,2;

SELECT table_name,column_name,data_type,character_maximum_length,
       numeric_precision,numeric_scale,is_nullable,column_default,is_identity
FROM information_schema.columns WHERE table_schema='public'
ORDER BY table_name,ordinal_position;

SELECT c.relname,con.conname,con.contype,con.convalidated,
       pg_get_constraintdef(con.oid)
FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid
JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname='public' ORDER BY c.relname,con.contype,con.conname;

SELECT tablename,indexname,indexdef FROM pg_indexes
WHERE schemaname='public' ORDER BY tablename,indexname;
SELECT count(*) AS index_invalides FROM pg_index i
JOIN pg_class c ON c.oid=i.indexrelid
WHERE c.relnamespace='public'::regnamespace AND (NOT indisvalid OR NOT indisready);
SELECT event_object_table,trigger_name,action_timing,event_manipulation,action_statement
FROM information_schema.triggers WHERE trigger_schema='public';
SELECT count(*) AS vues FROM information_schema.views WHERE table_schema='public';
SELECT count(*) AS politiques_rls FROM pg_policies WHERE schemaname='public';
SELECT sequencename,data_type,start_value,increment_by,last_value
FROM pg_sequences WHERE schemaname='public' ORDER BY sequencename;
SELECT installed_rank,version,description,type,script,checksum,success
FROM flyway_schema_history ORDER BY installed_rank;

-- \gexec n'exécute ici que des SELECT produits avec identifiants échappés.
SELECT format('SELECT %L AS table_name, count(*) AS lignes FROM %I.%I;',
              table_name,table_schema,table_name)
FROM information_schema.tables
WHERE table_schema='public' AND table_type='BASE TABLE'
ORDER BY table_name \gexec

-- Toutes les FK actuelles sont à une colonne ; signaler séparément toute
-- future FK composée avant de réutiliser ce contrôle générique.
SELECT conname AS fk_composee_a_controler FROM pg_constraint
WHERE contype='f' AND connamespace='public'::regnamespace AND cardinality(conkey)<>1;
SELECT format(
  'SELECT %L AS contrainte,count(*) AS orphelins FROM %s src LEFT JOIN %s dst ON src.%I=dst.%I WHERE src.%I IS NOT NULL AND dst.%I IS NULL;',
  c.conname,c.conrelid::regclass,c.confrelid::regclass,
  a.attname,b.attname,a.attname,b.attname)
FROM pg_constraint c
JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=c.conkey[1]
JOIN pg_attribute b ON b.attrelid=c.confrelid AND b.attnum=c.confkey[1]
WHERE c.contype='f' AND c.connamespace='public'::regnamespace
  AND cardinality(c.conkey)=1 ORDER BY c.conname \gexec
COMMIT;
SQL
```

## Annexe B — Rejouer les contrôles de cohérence et de préservation

Ce bloc cible le schéma **V1 actuellement observé**. Après changement des colonnes historiques, adapter les jointures et sélectionner explicitement les colonnes d’origine pour comparer les empreintes. Les intersections ci-dessous utilisent des dates de fin inclusives et assimilent une fin NULL à l’infini ; les compter ne constitue pas une décision d’interdire ces intersections.

```bash
docker compose exec -T \
  -e PGOPTIONS='-c default_transaction_read_only=on -c statement_timeout=15000 -c lock_timeout=3000' \
  postgres psql -X -P pager=off -v ON_ERROR_STOP=1 \
  -U gestion_eleves -d gestion_eleves <<'SQL'
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT current_setting('transaction_read_only'), current_timestamp;

SELECT id_classe,id_matiere,count(*) AS enseignements,
       count(DISTINCT coefficient_matiere) AS coefficients_distincts,
       min(coefficient_matiere) AS coefficient_min,
       max(coefficient_matiere) AS coefficient_max,
       count(*) FILTER (WHERE actif) AS actifs
FROM enseignement GROUP BY id_classe,id_matiere ORDER BY 1,2;
SELECT id_classe,annee_scolaire FROM classe ORDER BY 1;

SELECT p.id_periode,p.date_debut,p.date_fin,p.date_debut_saisie,
       p.date_fin_saisie,p.statut,
       array_agg(DISTINCT c.annee_scolaire)
         FILTER (WHERE c.annee_scolaire IS NOT NULL) AS annees_via_evaluations
FROM periode p LEFT JOIN evaluation ev USING(id_periode)
LEFT JOIN enseignement en USING(id_enseignement)
LEFT JOIN classe c USING(id_classe)
GROUP BY p.id_periode ORDER BY p.id_periode;

SELECT a.id_periode AS periode_a,b.id_periode AS periode_b,
       a.libelle=b.libelle AS meme_libelle,
       a.date_debut=b.date_debut AND a.date_fin=b.date_fin AS memes_dates
FROM periode a JOIN periode b
  ON a.id_periode<b.id_periode
 AND a.date_debut<=b.date_fin AND b.date_debut<=a.date_fin ORDER BY 1,2;

SELECT 'coefficients_contradictoires' AS controle,count(*) AS anomalies
FROM (SELECT id_classe,id_matiere FROM enseignement GROUP BY 1,2
      HAVING count(DISTINCT coefficient_matiere)>1) t
UNION ALL
SELECT 'coefficients_non_positifs',count(*) FROM enseignement
WHERE coefficient_matiere<=0
UNION ALL
SELECT 'periodes_chevauchement_toutes_annees',count(*)
FROM periode a JOIN periode b ON a.id_periode<b.id_periode
 AND a.date_debut<=b.date_fin AND b.date_debut<=a.date_fin
UNION ALL
SELECT 'scolarites_chevauchement_meme_eleve',count(*)
FROM scolarite a JOIN scolarite b
 ON a.id_eleve=b.id_eleve AND a.id_scolarite<b.id_scolarite
AND a.date_debut<=coalesce(b.date_fin,'infinity'::date)
AND b.date_debut<=coalesce(a.date_fin,'infinity'::date)
UNION ALL
SELECT 'enseignements_chevauchement_meme_classe_matiere',count(*)
FROM enseignement a JOIN enseignement b
 ON a.id_enseignement<b.id_enseignement
AND a.id_classe=b.id_classe AND a.id_matiere=b.id_matiere
AND a.date_debut<=coalesce(b.date_fin,'infinity'::date)
AND b.date_debut<=coalesce(a.date_fin,'infinity'::date)
UNION ALL
SELECT 'notes_classe_differente_evaluation',count(*)
FROM note n JOIN scolarite s USING(id_scolarite)
JOIN evaluation ev USING(id_evaluation)
JOIN enseignement en USING(id_enseignement) WHERE s.id_classe<>en.id_classe
UNION ALL
SELECT 'notes_hors_dates_scolarite',count(*)
FROM note n JOIN scolarite s USING(id_scolarite)
JOIN evaluation ev USING(id_evaluation)
WHERE ev.date_evaluation<s.date_debut
   OR ev.date_evaluation>coalesce(s.date_fin,'infinity'::date)
UNION ALL
SELECT 'notes_hors_bareme',count(*)
FROM note n JOIN evaluation ev USING(id_evaluation)
WHERE n.valeur<0 OR n.valeur>ev.bareme
UNION ALL
SELECT 'evaluations_hors_periode',count(*)
FROM evaluation ev JOIN periode p USING(id_periode)
WHERE ev.date_evaluation NOT BETWEEN p.date_debut AND p.date_fin
UNION ALL
SELECT 'evaluations_hors_enseignement',count(*)
FROM evaluation ev JOIN enseignement en USING(id_enseignement)
WHERE ev.date_evaluation<en.date_debut
   OR ev.date_evaluation>coalesce(en.date_fin,'infinity'::date)
UNION ALL
SELECT 'periodes_annees_multiples_via_evaluations',count(*)
FROM (SELECT ev.id_periode FROM evaluation ev
      JOIN enseignement en USING(id_enseignement) JOIN classe c USING(id_classe)
      GROUP BY 1 HAVING count(DISTINCT c.annee_scolaire)>1) t
UNION ALL
SELECT 'periodes_sans_annee_via_evaluations',count(*)
FROM periode p WHERE NOT EXISTS
  (SELECT 1 FROM evaluation ev WHERE ev.id_periode=p.id_periode)
UNION ALL
SELECT 'scolarites_sans_inscription_couvrante',count(*)
FROM scolarite s WHERE NOT EXISTS
  (SELECT 1 FROM inscription i WHERE i.id_eleve=s.id_eleve
   AND i.date_inscription<=s.date_debut
   AND coalesce(i.date_fin,'infinity'::date)>=coalesce(s.date_fin,'infinity'::date))
UNION ALL
SELECT 'personnes_enseignant_et_responsable',count(*)
FROM enseignant e JOIN responsable r USING(id_personne)
UNION ALL
SELECT 'personnes_sans_specialisation',count(*) FROM personne p
WHERE NOT EXISTS(SELECT 1 FROM eleve e WHERE e.id_personne=p.id_personne)
  AND NOT EXISTS(SELECT 1 FROM enseignant e WHERE e.id_personne=p.id_personne)
  AND NOT EXISTS(SELECT 1 FROM responsable r WHERE r.id_personne=p.id_personne)
UNION ALL
SELECT 'comptes_role_enseignant_sans_profil',count(*) FROM compte_utilisateur c
WHERE c.role='ENSEIGNANT' AND NOT EXISTS
  (SELECT 1 FROM enseignant e WHERE e.id_personne=c.id_personne)
UNION ALL
SELECT 'comptes_role_responsable_sans_profil',count(*) FROM compte_utilisateur c
WHERE c.role='RESPONSABLE' AND NOT EXISTS
  (SELECT 1 FROM responsable r WHERE r.id_personne=c.id_personne)
UNION ALL
SELECT 'bulletins_doublons_eleve_periode_apres_transformation',count(*)
FROM (SELECT s.id_eleve,b.id_periode FROM bulletin b
      JOIN scolarite s USING(id_scolarite) GROUP BY 1,2 HAVING count(*)>1) t
UNION ALL
SELECT 'lignes_doublons_bulletin_matiere_apres_transformation',count(*)
FROM (SELECT l.id_bulletin,en.id_matiere FROM ligne_bulletin l
      JOIN enseignement en USING(id_enseignement) GROUP BY 1,2 HAVING count(*)>1) t;

SELECT 'fenetres_saisie_partielles' AS controle,count(*) AS anomalies FROM periode
WHERE (date_debut_saisie IS NULL)<>(date_fin_saisie IS NULL)
UNION ALL
SELECT 'notes_hors_fenetre_saisie_renseignee',count(*)
FROM note n JOIN evaluation e USING(id_evaluation) JOIN periode p USING(id_periode)
WHERE (p.date_debut_saisie IS NOT NULL
       AND (n.date_saisie AT TIME ZONE 'Europe/Paris')::date<p.date_debut_saisie)
   OR (p.date_fin_saisie IS NOT NULL
       AND (n.date_saisie AT TIME ZONE 'Europe/Paris')::date>p.date_fin_saisie)
UNION ALL
SELECT 'evaluations_enseignement_inactif',count(*)
FROM evaluation e JOIN enseignement en USING(id_enseignement) WHERE NOT en.actif
UNION ALL
SELECT 'notes_scolarite_non_active',count(*)
FROM note n JOIN scolarite s USING(id_scolarite) WHERE s.statut<>'ACTIVE';

SELECT 'note' AS table_name,statut_note AS statut,count(*) FROM note GROUP BY statut_note
UNION ALL SELECT 'scolarite',statut,count(*) FROM scolarite GROUP BY statut
UNION ALL SELECT 'inscription',statut,count(*) FROM inscription GROUP BY statut
UNION ALL SELECT 'periode',statut,count(*) FROM periode GROUP BY statut
UNION ALL SELECT 'compte_utilisateur',role,count(*) FROM compte_utilisateur GROUP BY role;
SELECT count(*) AS comptes_actifs,
       count(*) FILTER(WHERE role='ADMIN') AS administrateurs_actifs
FROM compte_utilisateur WHERE actif;

-- Les contenus ne sont pas affichés, seulement une empreinte par table.
SELECT format(
  'SELECT %L AS table_name,md5(coalesce(string_agg(to_jsonb(t)::text,chr(10) ORDER BY %I),'''')) AS empreinte_contenu FROM %I t;',
  t,k,t)
FROM (VALUES ('enseignement','id_enseignement'),('scolarite','id_scolarite'),
             ('evaluation','id_evaluation'),('note','id_note'),
             ('bulletin','id_bulletin'),('ligne_bulletin','id_ligne_bulletin')) q(t,k)
\gexec
COMMIT;
SQL
```

## 11. Premier lot recommandé et synthèse opérationnelle

**Premier lot : isoler les tests de migration PostgreSQL 17, puis V2 PONDERER et sa reprise contrôlée, sans suppression de l’ancien coefficient.** Adapter ensuite le calcul pour lire PONDERER et grouper par matière, avec une API d’administration classe–matière et son formulaire. Garder les contrats transitoires explicitement documentés et une seule source d’écriture. Le retrait du coefficient de l’enseignement attend le lot de bascule et sa vérification finale.

Critères d’acceptation du premier lot : V1 inchangée ; reprise des 3 couples à 2.00 sur la copie de l’état audité ; même contenu des 3 notes/évaluations/scolarités ; conflit artificiel détecté et transaction annulée ; calcul inchangé à données équivalentes ; une seule ligne par matière en présence de deux enseignements ; accès à l’administration des coefficients contrôlé.

- **Déjà compatible** : identifiants et liens pédagogiques, coefficients/barèmes d’évaluation, schémas personne/profils côté SQL, responsabilité légale, référent de classe, données présentes sans orphelins ni incohérence de classe/barème/dates détectée.
- **À changer** : propriété des coefficients, mapping Java des profils, année de période, habilitation, motifs d’exception, rattachement/versionnement/validation des bulletins, lignes par matière, gel et archivage PDF, contrats et écrans associés.
- **À risque ou à décider** : périodes superposées, absence d’inscriptions, coefficients lors d’un changement de classe, règles de validation/publication et statuts, immutabilité historique, concurrence et coexistence de versions applicatives.
- **Automatisable immédiatement dans le prochain développement** : contrôles SQL en lecture seule de l’annexe, inventaire du schéma, comparaison des données d’origine, isolation des tests et reprise non ambiguë PONDERER ; ajout de colonnes facultatives/du booléen false après préparation des lots. Aucun de ces changements de schéma n’a été exécuté dans cet audit.
