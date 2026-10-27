# Lot 2 — Audit de l’héritage PERSONNE et des profils

Audit du **27 octobre 2026**, dépôt `Florian2524/gestion-eleves`, branche `main`, commit observé `affcc1573a33f40b412eadd2938d62d15350c4e7`.

**Conclusion : le lot 2 peut être réalisé sans migration SQL.** Les tables PostgreSQL actuelles permettent déjà une personne sans profil et plusieurs profils portant le même `id_personne`. La refonte concerne le mapping Java/JPA, les opérations métier, la validation des rôles et les parcours React. Cette compatibilité structurelle a été vérifiée sur PostgreSQL local ; le futur mapping Hibernate reste à valider par des tests sur une instance isolée.

## 1. Diagnostic de l’architecture actuelle

### 1.1 Périmètre et nature des preuves

Sources : [AUDIT_MIGRATION.md](AUDIT_MIGRATION.md), notamment §4.1, [MLD Looping](MLD.png), entités, tous les repositories, services, mappers, DTO, contrôleurs et classes de sécurité sous [backend](../../backend/src/main/java/fr/afpa/backend/), interfaces sous [frontend/src](../../frontend/src/), tests et scripts d’isolation. La règle métier explicite **partielle et recouvrante** prévaut sur le `XT` technique de l’export Looping signalé dans l’audit initial.

Les constats Java/React ci-dessous proviennent de lectures et recherches statiques. Une transaction PostgreSQL a été exécutée le **27 octobre à 09:29:52 UTC (10:29:52 Europe/Paris)**, sur `gestion_eleves`, PostgreSQL **17.10** : `psql -X`, `ON_ERROR_STOP=1`, `default_transaction_read_only=on`, délais de requête/verrou de 15 s/3 s, `BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY`, puis `ROLLBACK`. `transaction_read_only=on` a été confirmé. L’accès Docker a nécessité une sortie du confinement pour cette commande ciblée en lecture seule.

Aucun démarrage Spring Boot, test, migration, DDL, écriture de données, appel de séquence, commit Git ou push. Seul ce document est créé. Aucun nom, courriel, identifiant personnel ni secret de compte n’est reproduit. Les recommandations ne sont pas une implémentation ni un résultat de test Hibernate.

### 1.2 Un modèle SQL recouvrant, une hiérarchie Java exclusive

[Personne.java](../../backend/src/main/java/fr/afpa/backend/entity/Personne.java) est une entité concrète avec `@Inheritance(strategy = JOINED)`. Elle porte `idPersonne` généré par `IDENTITY`, `nom`, `prenom`, `emailContact`, `telephone` et `adresse`. [Eleve.java](../../backend/src/main/java/fr/afpa/backend/entity/Eleve.java), [Enseignant.java](../../backend/src/main/java/fr/afpa/backend/entity/Enseignant.java) et [Responsable.java](../../backend/src/main/java/fr/afpa/backend/entity/Responsable.java) utilisent `extends Personne` et `@PrimaryKeyJoinColumn(id_personne)`.

La personne seule est déjà représentable. En revanche, une instance Java ne peut pas être simultanément deux classes sœurs `Enseignant` et `Responsable`. Une lecture polymorphe par `PersonneRepository` ou via `CompteUtilisateur.personne` ne fournit donc pas une représentation fiable du cumul métier. Les symptômes précis du mapping actuel en présence de deux lignes spécialisées n’ont pas été provoqués sur la base locale : **ne pas y créer un profil double avant la bascule Java**.

Les autres entités référencent déjà le bon objet métier : `Classe.professeurPrincipal` et `Enseignement.enseignant` ciblent `Enseignant`, `Inscription.eleve` et `Scolarite.eleve` ciblent `Eleve`, et `ResponsabiliteLegale` relie `Responsable` à `Eleve`. Leurs FK doivent rester spécialisées. `CompteUtilisateur.personne` cible correctement `Personne`. `Bulletin.validateur` cible actuellement `Enseignant` : sa généralisation à `Personne` appartient à un autre lot et demanderait un changement de FK.

### 1.3 Créations, modifications et attributs hérités

| Usage actuel | Localisation | Conséquence de la composition |
|---|---|---|
| `new Eleve(...)`, `new Enseignant(...)`, `new Responsable(...)` avec identité et données de profil | `EleveMapper.toEntity`, `EnseignantMapper.toEntity`, `ResponsableMapper.toEntity` | Chaque création actuelle génère une nouvelle personne. Faire résoudre/créer la personne par le service, puis construire le profil avec cette personne |
| `super(nom, prenom, emailContact, telephone, adresse)` | Constructeurs des trois profils | Remplacer par une association obligatoire ; aucun profil ne doit fabriquer implicitement une seconde identité lors d’un rattachement |
| Lecture des cinq coordonnées et de `getIdPersonne()` directement sur le profil | Trois mappers de profils ; `ResponsabiliteLegaleMapper` lit noms/prénoms ; `BulletinCalculService` lit identité de l’élève | Lire `profil.getPersonne()` pour les coordonnées ; garder le même identifiant scalaire sur le profil |
| Écriture des cinq coordonnées par `profil.setNom/...` | `updateEntity` des trois mappers de profils | Modifier l’unique `Personne` gérée. La modification est visible dans tous ses profils et dans les réponses du compte |
| Lecture de `getIdPersonne()` dans les relations | `ClasseMapper`, `EnseignementMapper`, `ScolariteMapper`, `InscriptionMapper`, `ResponsabiliteLegaleMapper` | Les signatures peuvent rester compatibles si chaque profil conserve son attribut JPA scalaire `idPersonne` |
| `new Personne(...)` | `FirstAdminBootstrap.run` | Conserver : le premier ADMIN n’a pas besoin de profil |
| `Personne teacher = new Enseignant(...)` | `CompteUtilisateurServiceTest` | Ne compilera plus ; les fixtures devront créer séparément la personne et son profil |

`EleveMapper.toResponse` transforme une référence photo interne non nulle en `/eleves/{idPersonne}/photo`. Son `updateEntity` ne change pas la photo ; les mutations photo passent par `ElevePhotoService`. Conserver cette séparation et les URL existantes.

L’identité partagée rend visible une conséquence aujourd’hui masquée : éditer le nom depuis l’écran « Responsables » change aussi le nom de l’enseignant. L’ajout d’un profil à une personne existante doit accepter uniquement les données spécialisées, sans écraser ses coordonnées avec des champs vides ou périmés.

### 1.4 Inventaire des `instanceof`

La recherche couvre `backend/src`, y compris les tests. Les **deux seuls tests de type liés à cet héritage métier** sont dans `CompteUtilisateurService.validateRole` : `personne instanceof Enseignant` et `personne instanceof Responsable`. Les remplacer respectivement par l’existence du profil enseignant/responsable pour `personne.idPersonne`. ADMIN reste autorisé sans profil.

Les autres `instanceof` sont indépendants de l’héritage et doivent être conservés :

| Fichier | Objet testé |
|---|---|
| `security/ActiveAccountJwtValidator.java` | Claims numériques du compte et de la personne |
| `security/SecurityExpressions.java` | Principal `CompteUtilisateurPrincipal`, principal `Jwt`, claim numérique |
| `service/AuthenticationService.java` | Authentification anonyme et type du principal de sécurité |
| `entity/ResponsabiliteLegaleId.java`, `entity/PondererId.java` | Comparaison des clés dans `equals` |

Aucun `TYPE(...)`, `TREAT(...)`, cast explicite vers les profils ou autre sous-classe de `Personne` n’a été trouvé dans `backend/src`. Les tests de type sur les erreurs React (`ApiError`, etc.) n’appartiennent pas à cette refonte.

### 1.5 Suppressions et cascades actuelles

Aucun `CascadeType`, `orphanRemoval` ou `@OnDelete` n’est déclaré dans les entités actuelles. Cela ne rend pas les suppressions sûres : la suppression d’une entité de la hiérarchie JOINED concerne aussi sa ligne de base, et des cascades **SQL** existent.

| Opération actuelle | Garde applicative observée | Risque |
|---|---|---|
| `EleveService.delete` | Aucune vérification de référence | Suppression du profil et de la personne si les FK le permettent ; compte et liens légaux entraînés ; FK inscription/scolarité bloquantes |
| `ResponsableService.delete` | Aucune | Suppression de l’identité et du compte ; liens légaux effacés par cascade |
| `EnseignantService.delete` | Refus s’il existe un enseignement, actif ou historique | Oublie classes, bulletins validés/référencés et compte ; les FK `SET NULL` peuvent effacer le professeur principal et le validateur |
| `CompteUtilisateurService.delete/update` | Protection du dernier ADMIN actif, avec verrou sur les comptes ADMIN actifs | Protection à conserver ; elle n’intercepte pas une suppression de personne déclenchée ailleurs |
| `ResponsabiliteLegaleService.delete` | Suppression du seul lien composite | N’efface ni responsable ni élève ; continuer ainsi |

Une suppression de profil pourrait donc contourner la protection du dernier ADMIN si un compte ADMIN était rattaché à cette personne. Aucun tel scénario n’a été exécuté. Le compte local est conservé ; le risque est établi par le chemin de suppression et les FK, pas par un incident observé.

## 2. Fichiers Java et React impactés

Les chemins Java ci-dessous sont relatifs à [backend/src/main/java/fr/afpa/backend](../../backend/src/main/java/fr/afpa/backend/). **Modifier** signifie changement nécessaire ; **vérifier** signifie dépendance à couvrir par les tests, sans réécriture imposée si le contrat est conservé.

### 2.1 Entités, repositories et services

| Fichiers | Impact pour le lot 2 |
|---|---|
| `entity/Personne.java`, `Eleve.java`, `Enseignant.java`, `Responsable.java` | **Modifier** : suppression de l’héritage et PK partagées par association |
| `entity/CompteUtilisateur.java`, `RoleUtilisateur.java` | **Vérifier** : personne autonome ; rôle unique et compte unique par personne conservés ; aucun nouveau rôle |
| `entity/Classe.java`, `Enseignement.java`, `Inscription.java`, `Scolarite.java` | **Vérifier** : associations vers les profils et mêmes colonnes/FK ; aucun élargissement à toute personne |
| `entity/ResponsabiliteLegale.java`, `ResponsabiliteLegaleId.java` | **Vérifier** : les deux `@MapsId` existants et la clé composite survivent à la composition |
| `entity/Evaluation.java`, `Note.java`, `Bulletin.java`, `LigneBulletin.java` | **Vérifier** : chaînes pédagogiques et références historiques ; mapping du validateur inchangé dans ce lot |
| `repository/PersonneRepository.java` | **Modifier/étendre** : accès à une identité indépendante, sélection/recherche administrative et verrou de cycle de vie |
| `repository/EleveRepository.java`, `EnseignantRepository.java`, `ResponsableRepository.java` | **Vérifier/étendre** : `existsById` pour profils, chargement de personne, conservation des contrôles matricule/numéro d’employé |
| `repository/CompteUtilisateurRepository.java` | **Étendre** pour contrôler le rôle du compte avant retrait de profil ; vérifier `existsByPersonneIdPersonne`, l’EntityGraph `personne` et les verrous ADMIN |
| `repository/ClasseRepository.java`, `EnseignementRepository.java`, `EvaluationRepository.java`, `NoteRepository.java`, `ScolariteRepository.java`, `ResponsabiliteLegaleRepository.java` | **Vérifier** tous les JPQL et chemins dérivés `...IdPersonne`, comparaisons d’entités et requêtes d’autorisation ; ajouter les recherches de dépendances utiles aux suppressions |
| `repository/InscriptionRepository.java`, `BulletinRepository.java` | **Étendre** pour les garde-fous élève/validateur ; tester aussi les scolarités et classes référentes |
| `repository/LigneBulletinRepository.java` | **Vérifier** la protection indirecte des enseignements historiques |
| `service/EleveService.java`, `EnseignantService.java`, `ResponsableService.java` | **Modifier** : création identité+profil atomique, ajout sur identité existante, mise à jour partagée, suppression indépendante et protégée |
| `service/CompteUtilisateurService.java` | **Modifier** : validation des profils par repository ; cohérence avec le retrait concurrent d’un profil ; préserver les protections ADMIN |
| `service/FirstAdminBootstrap.java` | **Vérifier** : création de Personne seule, absence d’action quand un compte existe ; aucune recréation du compte local |
| `service/AuthenticationService.java` | **Vérifier** login/register ; aucun droit supplémentaire issu des profils |
| `service/ResponsabiliteLegaleService.java`, `ClasseService.java`, `EnseignementService.java` | **Vérifier/adapter** résolution des profils et protocole de verrouillage des affectations face aux suppressions |
| `service/InscriptionService.java`, `ScolariteService.java`, `EvaluationService.java`, `NoteService.java` | **Vérifier** références, contrôles classe/barème, autorisations et erreurs de dépendance ; conserver les PK et l’historique |
| `service/ElevePhotoService.java` | **Vérifier** lecture/mutation par même ID, conservation du stockage et nettoyage éventuel après suppression réussie du profil |
| `service/BulletinCalculService.java` | **Adapter** accès à l’identité de l’élève si les anciens accesseurs sont retirés ; aucune modification de formule |
| `service/BulletinPdfService.java` | **Vérifier** : consomme le DTO calculé ; identité, résultats, téléchargement autorisé |

`Matiere`, `Periode`, `Ponderer`, `PondererId` et leurs repositories/services n’utilisent pas l’héritage de personne. Le lot 2 n’impose pas de changement de leur code. Les tests du lot 1 restent requis pour protéger leurs interactions avec enseignement et calcul.

### 2.2 Mappers, DTO, contrôleurs et sécurité

| Fichiers | Impact |
|---|---|
| `mapper/EleveMapper.java`, `EnseignantMapper.java`, `ResponsableMapper.java` | **Modifier** constructeurs et coordonnées ; conserver les réponses JSON plates |
| `mapper/ResponsabiliteLegaleMapper.java` | **Adapter** noms/prénoms à la composition, ou vérifier les délégations transitoires |
| `mapper/CompteUtilisateurMapper.java`, `ClasseMapper.java`, `EnseignementMapper.java`, `ScolariteMapper.java`, `InscriptionMapper.java` | **Vérifier** les lectures d’identité/ID ; les signatures peuvent rester identiques |
| `mapper/NoteMapper.java`, `EvaluationMapper.java` | **Vérifier** références indirectes ; aucune dépendance aux coordonnées héritées |
| `dto/eleve/EleveRequest.java`, `EleveResponse.java` ; `dto/enseignant/EnseignantRequest.java`, `EnseignantResponse.java` ; `dto/responsable/ResponsableRequest.java`, `ResponsableResponse.java` | Conserver les contrats actuels ; ajouter de préférence des DTO distincts pour rattacher un profil, plutôt que rendre ambigus les POST existants |
| `dto/compteutilisateur/CompteUtilisateurCreateRequest.java`, `CompteUtilisateurUpdateRequest.java`, `CompteUtilisateurResponse.java` ; `dto/authentication/RegisterRequest.java`, `LoginRequest.java`, `AuthenticationResponse.java` | **Vérifier** IDs et rôle ; pas de profils déduits du rôle ; ne pas exposer le hash |
| `dto/responsabilitelegale/ResponsabiliteLegaleRequest.java`, `ResponsabiliteLegaleResponse.java` ; `dto/classe/ClasseRequest.java`, `ClasseResponse.java` ; `dto/enseignement/EnseignementRequest.java`, `EnseignementResponse.java` ; `dto/inscription/InscriptionRequest.java`, `InscriptionResponse.java` ; `dto/scolarite/ScolariteRequest.java`, `ScolariteResponse.java` | **Vérifier** IDs de profils inchangés ; aucun objet JPA imbriqué dans l’API |
| `dto/note/NoteRequest.java`, `NoteResponse.java`, `dto/evaluation/EvaluationRequest.java`, `EvaluationResponse.java`, `dto/bulletin/BulletinDto.java`, `LigneBulletinDto.java` | **Vérifier** contrats pédagogiques, noms de l’élève et résultats ; pas d’évolution de structure nécessaire |
| `controller/EleveController.java`, `EnseignantController.java`, `ResponsableController.java` | **Adapter** les parcours de rattachement retenus et les suppressions ; conserver URI et `Location` basés sur `idPersonne` |
| `controller/CompteUtilisateurController.java`, `AuthenticationController.java`, `ResponsabiliteLegaleController.java` | **Vérifier** création de compte, profil requis et gestion des liens ; l’inscription d’un compte reste réservée à ADMIN dans le service |
| `controller/ClasseController.java`, `EnseignementController.java`, `EvaluationController.java`, `NoteController.java`, `ScolariteController.java`, `InscriptionController.java`, `BulletinController.java` | **Vérifier** filtres de listes, autorisations individuelles, mêmes identifiants et routes |
| `security/CompteUtilisateurPrincipal.java`, `CompteUtilisateurDetailsService.java`, `JwtService.java`, `ActiveAccountJwtValidator.java`, `SecurityExpressions.java` ; `config/SecurityConfig.java`, `JwtConfig.java` | **Vérifier**, et adapter chargements/règles d’URL des ajouts ; rôle unique et périmètres existants inchangés |
| `exception/GlobalExceptionHandler.java`, `ResourceInUseException.java`, `DuplicateResourceException.java`, `InvalidAccountRoleException.java` | Réutiliser les réponses existantes ; compléter les messages de conflit de profils, aujourd’hui parfois orientés coefficients |

À créer lors d’une future implémentation, selon le contrat retenu : service de gestion des personnes/profils, DTO de sélection minimale et d’ajout de profil, éventuellement contrôleur administratif de personnes. Aucun de ces fichiers n’est créé par cet audit.

### 2.3 React et appels API

Chemins relatifs à [frontend/src](../../frontend/src/).

| Fichier | Constat et adaptation |
|---|---|
| `pages/AdministrationPages.jsx` | Formulaires enseignants/responsables saisissent toujours une nouvelle identité ; ajouter choix « nouvelle personne / personne existante », afficher les profils déjà présents, limiter le rattachement aux attributs spécialisés. Classes et enseignements continuent à sélectionner exclusivement les enseignants |
| `components/school/ResourcePage.jsx` | CRUD générique construit le JSON depuis `fields`, sans parcours de rattachement. Étendre de façon ciblée ou fournir un formulaire dédié ; le seul ajout d’un champ caché ne suffit pas. Clarifier que supprimer retire un profil ; afficher les refus 409 |
| `pages/ComptesUtilisateursPage.jsx` | Concatène enseignants et responsables avec un unique `kind` ; pour un double profil sans compte, notamment en sélection ADMIN, options et clés React `idPersonne` seront dupliquées. Regrouper par ID, conserver l’ensemble des profils disponibles, filtrer selon le rôle demandé et exclure une seule fois les personnes déjà titulaires d’un compte |
| `pages/ComptesUtilisateursPage.jsx` | Ne propose actuellement aucune personne sans profil, même pour ADMIN, alors que le backend le permet. Un sélecteur administratif de personnes doit inclure ces personnes si ce parcours est retenu ; ne pas imposer un profil fictif |
| `pages/ResponsabilitesLegalesPage.jsx` | Sélection par `idResponsable/idEleve`, affichage des noms plats ; un enseignant ayant reçu un profil responsable doit apparaître dans les responsables avec le même ID. Ne pas confondre création du profil et création d’un lien légal |
| `pages/ElevesPage.jsx` | CRUD, nom/prénom, tri/recherche et photo par `idPersonne` ; préserver JSON/URL et information sur l’effet partagé d’une édition de coordonnées. Ajouter la réutilisation d’identité si le parcours est ouvert aux élèves |
| `components/school/schoolData.js` | Labels et lookups de profils par `idPersonne` : conserver ; ajouter la source administrative des personnes si nécessaire, sans la charger pour ENSEIGNANT/RESPONSABLE |
| `api/schoolApi.js`, `api/adminApi.js`, `api/elevesApi.js` | Ajouter appels de sélection/rattachement si retenus ; conserver appels CRUD et photo existants |
| `pages/NotesPage.jsx`, `api/notesApi.js` | Contrôle indirect : rapprochement `eleve.idPersonne = scolarite.idEleve`, filtrage et droits de saisie selon le rôle ; pas de réécriture nécessaire si les contrats restent plats |
| `pages/BulletinsPage.jsx`, `api/bulletinsApi.js` | Sélection par scolarité, identité de l’élève et calcul/PDF inchangés. Les clés des lignes sont déjà `idMatiere` depuis le lot 1 |
| `context/AuthContext.jsx`, `context/authContextCore.js`, `api/authStorage.js`, `api/authApi.js`, `api/http.js` | Contrôle de non-régression : réponse de login et stockage de `role`, `idPersonne`, `idUtilisateur`, token ; gestion 401/403 ; aucun besoin de nouvelle liste de rôles |
| `components/auth/ProtectedRoute.jsx`, `App.jsx`, `pages/DashboardPage.jsx`, `components/layout/AppHeader.jsx`, `pages/LoginPage.jsx` | Routes, menus et connexion restent fondés sur le rôle du compte. Adapter seulement les éventuelles routes administratives ; ne pas attribuer l’accès responsable par la présence d’un profil |

### 2.4 Tests et configuration à préserver

Les constructeurs de profils sont directement employés dans `service/EleveServiceTest.java`, `EnseignantServiceTest.java`, `ResponsableServiceTest.java`, `ElevePhotoServiceTest.java`, `ClasseServiceTest.java`, `EnseignementServiceTest.java`, `InscriptionServiceTest.java`, `ScolariteServiceTest.java` et `CompteUtilisateurServiceTest.java`, sous [backend/src/test/java/fr/afpa/backend](../../backend/src/test/java/fr/afpa/backend/). Adapter ces fixtures à la composition. Vérifier aussi les mocks d’`Eleve` dans `BulletinCalculServiceTest.java`, ceux de personnes/comptes dans les tests de sécurité, `ResponsabiliteLegaleServiceTest.java`, `FirstAdminBootstrapTest.java`, `BulletinPdfServiceTest.java`, les tests de contrôleurs et les jeux SQL des tests PONDERER.

Conserver l’isolation existante : [IsolatedPostgres.java](../../backend/src/test/java/fr/afpa/backend/support/IsolatedPostgres.java), [application.yaml de test](../../backend/src/test/resources/application.yaml), [test-ponderer.sh](../../scripts/test-ponderer.sh), [test-e2e.sh](../../scripts/test-e2e.sh) et [CI](../../.github/workflows/ci.yml). **Ces fichiers ont évolué depuis l’audit initial** : le script backend utilise désormais PostgreSQL 17 jetable, sans volume Compose, une URL explicite et un port différent de 5432 ; la configuration par défaut échoue sur un port fermé. Le script E2E isole également base, processus et photos. Aucun de ces scripts n’a été lancé ici.

## 3. Architecture JPA cible recommandée

### 3.1 Identité autonome et trois associations propriétaires

Conserver `Personne` comme `@Entity @Table(name = "personne")`, concrète, avec ses six attributs actuels et son générateur `IDENTITY`. Retirer `@Inheritance`. Chaque profil devient une entité indépendante, sans `extends Personne`, sans `@PrimaryKeyJoinColumn` et avec :

- `@Id @Column(name = "id_personne") private Long idPersonne`, **sans générateur** ;
- `@OneToOne(fetch = LAZY, optional = false) @MapsId` sur `private Personne personne` ;
- `@JoinColumn(name = "id_personne", nullable = false, foreignKey = ...)` avec le nom de FK actuel ;
- ses seuls attributs spécialisés actuels et un constructeur acceptant une `Personne` non nulle ;
- aucun `cascade = REMOVE/ALL`, aucun `orphanRemoval`. Recommandation initiale : aucune cascade, persistance explicite de la personne puis du profil.

Chaque profil est propriétaire de sa FK. Il n’est pas nécessaire d’ajouter des références inverses dans `Personne` : rechercher les profils par leur ID commun suffit. Cela évite un graphe bidirectionnel avec règles de suppression et chargements supplémentaires. Une personne peut ainsi exister avec zéro, un ou plusieurs profils ; le caractère obligatoire de `profil.personne` n’impose pas de profil à la personne. Les identifiants égaux correspondent à des entités JPA de types distincts, partageant la même identité métier.

L’association à la personne est immuable une fois le profil créé : ne pas proposer de changement de personne par PUT ou setter public libre. Ne pas créer d’ID secondaire de profil, de colonne discriminante, de table générale de profils ni d’`@EmbeddedId` pour ces PK simples. `ResponsabiliteLegale` garde sa clé composite existante.

Ce choix s’appuie sur le mapping d’[identité dérivée Hibernate avec `@MapsId`](https://docs.hibernate.org/orm/6.6/userguide/html_single/#identifiers-derived). Le détail du comportement doit être validé avec les dépendances réellement résolues du projet, sans mise à niveau de framework opportuniste.

### 3.2 Persistance, chargements et compatibilité

Le service doit charger la personne gérée dans sa transaction, contrôler les règles, puis persister le nouveau profil associé. Pour une nouvelle identité, persister d’abord `Personne`, puis le profil, dans **la même transaction** : l’échec du profil doit annuler l’identité nouvelle, sans exiger de continuité de la séquence.

Ne pas construire une `Personne` détachée avec un ID fourni par le client, ni utiliser un `merge` de graphe pour rattacher un profil. Avec une PK partagée, un ID prérempli peut modifier la détection d’une entité nouvelle par `save`. Choisir un chemin de création explicite par `EntityManager.persist` ou démontrer par test le comportement du repository utilisé ; ne pas ajouter une colonne `@Version` pour contourner ce point. [Spring Data JPA 3.5 décrit la distinction `persist`/`merge` et la détection par version/identifiant](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/entity-persistence.html).

Garder le champ persistant scalaire `idPersonne` sur chacun des profils permet de conserver les méthodes `...AndIdPersonneNot`, les chemins dérivés `...Enseignant_IdPersonne` et les JPQL `r.responsable.idPersonne`, `s.eleve.idPersonne`. Ils doivent être exécutés en tests ; leur réécriture systématique vers `personne.idPersonne` n’est pas nécessaire. Ne pas mapper des coordonnées dupliquées dans les tables de profils.

Préférer des mappers explicites vers `profil.personne` ; de petits accesseurs Java de délégation peuvent être une transition pour les appelants existants, sans conserver l’héritage ni créer d’attributs persistants supplémentaires. Tous les champs d’identité restent physiquement dans `personne`.

`open-in-view=false` est déjà configuré. Produire les DTO dans une transaction et prévoir `EntityGraph`/`join fetch personne` pour les listes de profils si nécessaire. Vérifier les chargements du compte au login et dans le validateur JWT, notamment hors transaction de service. Ne pas rendre tous les liens EAGER pour masquer un problème de durée de vie du contexte. Ne jamais sérialiser directement les nouvelles entités ou proxies dans les réponses.

### 3.3 Ajouter un profil responsable à une personne enseignante

Parcours recommandé, administrateur uniquement, à contractualiser avant implémentation :

1. Sélectionner explicitement une personne existante par `idPersonne`, avec les profils déjà présents. Un enseignant est un candidat valide.
2. Charger/verrouiller cette personne dans une transaction ; vérifier qu’elle existe et qu’aucun profil responsable n’est déjà présent.
3. Créer uniquement le profil `Responsable(personne, profession)` et le persister ; `@MapsId` fournit le même ID. Ne modifier ni la ligne enseignant ni les coordonnées ni le compte.
4. Renvoyer le `ResponsableResponse` actuel avec le même `idPersonne`. Une seconde création du même profil produit un conflit explicite, sans écraser la profession existante.
5. Créer séparément les liens `responsabilite_legale` souhaités. Posséder un profil responsable sans lien n’accorde aucune consultation d’élève.

Proposition additive : conserver les POST actuels `/eleves`, `/enseignants`, `/responsables` pour « nouvelle personne + profil », et ajouter des POST administratifs `/personnes/{idPersonne}/responsable`, `/enseignant`, `/eleve` avec DTO spécialisés. La sélection peut utiliser un GET `/personnes` paginé avec données minimales et profils disponibles. Ces routes sont **des propositions**, pas des contrats déjà décidés. La sécurité actuelle refuse les URL inconnues : les nouvelles routes devront être explicitement ADMIN.

Ne pas réutiliser automatiquement une personne par nom/prénom ou email de contact : ces attributs ne sont pas uniques et peuvent être partagés. Un résultat de recherche aide à sélectionner une identité, il ne prouve pas l’identité. Ne pas fusionner les doublons préexistants dans ce lot. Pour éviter les doublons de saisie à l’avenir, rendre le choix d’une personne existante accessible avant la création, avec confirmation métier du candidat.

### 3.4 Mise à jour et suppression indépendantes

Conserver les réponses plates, noms de champs, types d’ID, codes de réussite, URL de profils et photo. Les PUT existants peuvent continuer à modifier les coordonnées communes via la personne gérée, sous réserve d’indiquer cet effet dans l’interface et de tester la propagation. Une séparation ultérieure « modifier identité / modifier profil » peut être additive. L’ajout à une personne existante ne doit jamais réutiliser le PUT d’un autre profil pour changer sa nature.

Les DELETE de profils deviennent des suppressions **du seul profil**, après contrôles (§5). Laisser la personne exister même si c’était son dernier profil. Ne pas nettoyer automatiquement les « personnes sans profil », car elles sont valides et peuvent porter un compte. La suppression d’un compte ne supprime toujours pas son identité ou ses profils.

Ne pas exposer de DELETE général de `Personne` dans ce lot. Centraliser les opérations de cycle de vie et interdire les suppressions de masse de personnes/profils qui contournent ces règles. Préserver l’existence d’un autre profil ne signifie pas autoriser la perte des dépendances du profil supprimé.

## 4. Conséquences sur PostgreSQL et Flyway

### 4.1 Schéma réellement observé

| Table | Propriété vérifiée dans les catalogues PostgreSQL |
|---|---|
| `personne` | PK `id_personne BIGINT`, seule identité générée parmi les quatre tables de personnes/profils |
| `eleve` | PK et FK `id_personne → personne`, sans identité ni défaut ; matricule unique, naissance obligatoire, photo facultative |
| `enseignant` | PK et FK `id_personne → personne`, sans identité ni défaut ; numéro d’employé unique |
| `responsable` | PK et FK `id_personne → personne`, sans identité ni défaut ; profession facultative |
| `compte_utilisateur` | ID compte indépendant ; FK personne, unicité par personne et email de connexion ; rôle unique contrôlé dans `ADMIN/ENSEIGNANT/RESPONSABLE` |

Les PK assurent au plus un profil de chaque nature par personne. Aucun CHECK d’exclusion entre tables, aucune obligation de profil, aucun discriminant à convertir. Toutes les contraintes examinées sont validées. Les seuls triggers applicatifs observés sont les deux triggers PONDERER du lot 1 ; aucune RLS activée sur les cinq tables personnes/profils/comptes inspectées.

| FK actuelle | `ON DELETE` | Conséquence |
|---|---|---|
| `eleve/enseignant/responsable.id_personne → personne` | `CASCADE` | Effacer une personne efface ses profils ; effacer un profil n’efface **pas** sa personne |
| `compte_utilisateur.id_personne → personne` | `CASCADE` | Toute suppression de personne menace le compte |
| `classe.id_professeur_principal → enseignant` | `SET NULL` | Le SQL n’empêche pas la perte de la référence |
| `bulletin.id_validateur → enseignant` | `SET NULL` | Le SQL n’empêche pas la perte de l’auteur historique |
| `enseignement.id_enseignant → enseignant` | `NO ACTION` | Bloque la suppression d’un enseignant référencé, même enseignement inactif |
| `inscription.id_eleve`, `scolarite.id_eleve → eleve` | `NO ACTION` | Bloquent la suppression d’un élève référencé |
| `responsabilite_legale.id_responsable/id_eleve → profils` | `CASCADE` | Efface les liens légaux si un des profils est supprimé |

Ces comportements sont ceux de V1, inchangés par V2. La composition exploite leur direction correcte ; elle ne neutralise pas les effets d’un DELETE direct de personne.

### 4.2 État des données et historique Flyway

| Contrôle à l’instant de lecture | Résultat |
|---|---:|
| Personnes / élèves / enseignants / responsables | 8 / 4 / 3 / 0 |
| Personnes sans profil / multi-profils / enseignant+responsable | 1 / 0 / 0 |
| Comptes / ADMIN actifs | 1 / 1 |
| Orphelins élève/personne, enseignant/personne, responsable/personne, compte/personne | 0 pour chacun |
| Comptes ENSEIGNANT ou RESPONSABLE sans profil requis | 0 |
| Classes / enseignements / scolarités / évaluations / notes | 4 / 3 / 3 / 3 / 3 |
| Inscriptions / responsabilités légales / bulletins / lignes de bulletin | 0 / 0 / 0 / 0 |
| Pondérations | 3 |

La présence d’une personne sans profil est compatible avec l’existant. L’absence de double profil ne valide pas Hibernate pour ce cas ; il faut des fixtures nouvelles. Les tables historiques vides localement ne dispensent pas de tester des bulletins persistés et liens légaux non vides.

Flyway : **V1 et V2 réussies**, checksums enregistrés `-526686782` et `1440610697`. Empreintes SHA-256 des fichiers lus :

| Migration | SHA-256 |
|---|---|
| [V1__create_initial_schema.sql](../../backend/src/main/resources/db/migration/V1__create_initial_schema.sql) | `f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee` |
| [V2__create_ponderer.sql](../../backend/src/main/resources/db/migration/V2__create_ponderer.sql) | `35734eef70167a37c7f97749f364b88bdd10b6c7396df1c8a36e41548adb25f9` |

Ces lectures ne sont pas un lancement de `flyway validate` ni une validation du nouveau mapping. Aucun fichier Flyway ou historique n’a été changé.

### 4.3 Délimitation du « sans migration SQL »

**Aucun DDL, backfill, renumérotation, déplacement ou duplication de données n’est nécessaire pour la composition.** Conserver V1/V2 immuables, `ddl-auto=validate`, les noms physiques et toutes les PK/FK. Ne pas créer une V3 vide pour matérialiser ce lot et ne pas exécuter de `repair`, `clean` ou génération Hibernate du schéma.

La forme des quatre tables est compatible dès V1. L’application courante dépend déjà de PONDERER : la validation complète doit se faire sur **V1 puis V2**, ou sur une base déjà en V2 ; cela ne promet pas qu’elle fonctionne sur une base restée en V1 seule. Conserver le miroir `enseignement.coefficient_matiere`, la FK composée et les triggers de V2.

Les changements de validateur vers PERSONNE, habilitation, versions de bulletin, année de période, archivage métier avec nouvelles colonnes, ou restriction SQL des cascades sont des lots distincts. Ils peuvent demander des migrations ; ils ne sont pas nécessaires à la représentation des profils et ne doivent pas être introduits ici.

## 5. Risques de sécurité et d’intégrité

### 5.1 Profils métier et rôle d’accès restent distincts

Le système actuel a un seul compte possible par personne et **un seul rôle par compte**. `CompteUtilisateurPrincipal` expose une autorité `ROLE_...`. `JwtService` signe `sub`, `idUtilisateur`, `idPersonne` et `roles` contenant un seul rôle. `ActiveAccountJwtValidator` relit le compte avec sa personne et vérifie actif, sujet, identifiant de personne et rôle courant ; il exige une liste de taille 1. Il ne vérifie pas actuellement l’existence du profil métier.

Préserver ces règles et les secrets/hash existants. Ajouter un profil ne crée pas un compte, ne change pas son rôle et n’ajoute pas d’autorité au JWT. Pour une personne enseignante et responsable avec compte ENSEIGNANT, les consultations restent celles de l’enseignant ; la qualité de responsable n’ajoute pas automatiquement l’accès à ses enfants. Avec compte RESPONSABLE, seules les permissions responsable s’appliquent. Avec compte ADMIN, le rôle reste ADMIN. Accéder simultanément aux deux fonctions demanderait une décision explicite (§8).

`SecurityConfig` réserve enseignants, responsables, comptes, responsabilités légales et inscriptions à ADMIN ; les écritures des autres référentiels sont ADMIN, les écritures de notes/évaluations ADMIN ou ENSEIGNANT. `SecurityExpressions` limite les lectures à l’enseignement ou aux liens légaux de l’identité authentifiée. Les contrôleurs filtrent les listes et protègent les lectures individuelles ; le calcul/PDF de bulletin vérifie la scolarité. Conserver ces filtres et ces contrôles, y compris sur les photos et les IDs fournis dans les modifications.

`/auth/register` est permis au niveau HTTP mais le service exige une authentification ADMIN : préserver cette protection. Le futur sélecteur/rattachement de personnes doit être ADMIN côté backend ; masquer un bouton ou limiter React ne suffit pas. Ne jamais autoriser un responsable à se créer un lien légal ou un enseignant à s’attribuer un autre profil/compte par un DTO imbriqué.

### 5.2 Politique de suppression conservatrice recommandée

| Demande | Décision recommandée sans changement SQL |
|---|---|
| Retirer un profil non référencé | Autoriser ADMIN ; supprimer uniquement sa ligne, garder personne, autres profils et compte compatible |
| Retirer le profil requis par le rôle ENSEIGNANT/RESPONSABLE d’un compte, même inactif | Refuser 409 tant que le compte conserve ce rôle. Un changement de rôle doit être une opération administrative explicite et valide, jamais automatique |
| Retirer un profil d’une personne portant un compte ADMIN | Ne pas supprimer le compte ; autoriser uniquement si aucune autre dépendance ne s’y oppose ; le dernier ADMIN doit rester actif |
| Retirer un élève lié à une inscription, scolarité ou responsabilité légale | Refuser 409. Ne pas effacer scolarités, notes ou liens en cascade pour « débloquer » la suppression |
| Retirer un enseignant avec enseignement, classe référente ou bulletin validé/référencé | Refuser 409, même pour des données inactives ou anciennes. Ne pas laisser les `SET NULL` effacer silencieusement les références |
| Retirer un responsable possédant un lien légal | Refuser 409 tant que le lien existe ; sa révocation est une action distincte, explicitement autorisée |
| Retirer le dernier profil sans dépendance | Garder la personne seule ; aucune suppression automatique d’identité |
| Supprimer directement une personne ou utiliser un DELETE en masse | Aucun parcours dans ce lot ; interdire ce chemin dans les services de profils |

Pour une classe référente courante, une réaffectation explicite peut être envisagée avant retrait ; pour un auteur de bulletin historique, conserver la référence est la recommandation. Les règles d’effacement/archivage restent à décider. Le schéma actuel ne date pas les responsabilités légales : supprimer un lien ne conserve pas son historique. Refuser par défaut la suppression indirecte du lien évite cette perte involontaire sans inventer une table d’historique.

### 5.3 Transactions, concurrence et limites des garanties

- Sérialiser les ajouts/retraits de profils et changements de rôle d’une même personne par un verrou de cycle de vie sur `Personne`. Les services de comptes doivent participer au même protocole : un simple `existsById` avant insertion/suppression est insuffisant face à une transaction concurrente.
- Les affectations qui pourraient subir un `SET NULL` ou une cascade doivent suivre ce protocole également ; définir un ordre commun des verrous personne/profil/compte, compatible avec le verrou existant des ADMIN actifs. Prévoir des tests à deux transactions pour les interblocages et les courses.
- La PK du profil et les contraintes uniques restent le dernier rempart contre deux ajouts concurrents. Traduire le conflit en 409, sans écrasement par `merge`. Tester le rollback complet.
- Les FK `NO ACTION` protègent les références restantes, mais les `CASCADE` et `SET NULL` ne signalent pas les pertes qu’ils autorisent. **L’absence de migration SQL suppose que les chemins d’écriture respectent les garde-fous applicatifs.** Un écrivain SQL externe peut toujours supprimer une personne et entraîner son compte/profils. Garantir l’interdiction pour tout écrivain exigerait une décision supplémentaire sur les droits/FK/triggers.
- Ne pas laisser un compte perdre son profil requis : sinon un JWT inchangé peut encore passer la validation de compte, puisque celle-ci ne contrôle pas les profils. Le blocage du retrait est la politique minimale recommandée ; l’invalidation supplémentaire par contrôle de profil à chaque requête est une option à décider, pas un nouveau rôle.
- Conserver la protection du dernier ADMIN lors des changements de rôle, désactivations et suppressions de compte ; aucun changement de mot de passe, ID, email de connexion, activité ou date de création n’est nécessaire à cette refonte.

### 5.4 Historique pédagogique et fichiers

Conserver les chaînes `note → scolarite → eleve` et `note → evaluation → enseignement → enseignant`, ainsi que les liens classe/matière/période/PONDERER. Aucun transfert d’enseignement, de note ou de scolarité vers une nouvelle personne ne doit être fait pour contourner une suppression. L’existence du profil, et non le rôle de son compte, reste nécessaire pour une affectation métier.

Le calcul actuel lit PONDERER, regroupe les notes par matière, normalise sur 20 et pondère par coefficient d’évaluation ; précision de calcul 10, affichage à 2 décimales en `HALF_UP`, exclusion des matières sans moyenne du dénominateur général. Il conserve les filtres temporels d’enseignement et ne filtre pas les notes selon leur statut. Le lot 2 ne doit changer aucune de ces règles, ni l’identifiant d’enseignement de compatibilité choisi par le calcul.

Les bulletins servis sont aujourd’hui recalculés, y compris pour le PDF : modifier une identité peut changer l’en-tête d’un nouveau rendu, sans changer les moyennes. Le gel de documents officiels n’est pas acquis par cette refonte. Préserver également les références des bulletins/lignes éventuellement persistés, même si les tables locales sont vides.

Ne pas supprimer de photo en préparation d’une suppression de profil qui peut être refusée. Si un nettoyage de fichier est retenu pour un profil supprimable, l’effectuer après succès transactionnel et selon une politique explicite ; le stockage disque n’est pas transactionnel avec PostgreSQL. Aucun déplacement ou nettoyage de fichier n’est nécessaire à la bascule de mapping.

## 6. Plan d’implémentation progressif

Ce plan décrit un développement futur ; aucune étape d’implémentation n’a commencé dans cet audit.

| Étape | Travail borné | Condition de sortie |
|---|---|---|
| 1. Fixer les contrats et fixtures | Décider rattachement, édition commune et politique de retrait ; décrire les réponses compatibles ; préparer des fixtures anonymes à zéro/un/deux profils, comptes et historique | Parcours et refus attendus explicites ; aucune fusion automatique ni nouveau rôle |
| 2. Valider la composition sur PostgreSQL isolé | Quatre entités, constructeurs, persistance explicite ; lectures après flush/clear ; données V1→V2 | Mapping validé sans DDL supplémentaire ; ID partagés et relations historiques relus |
| 3. Adapter le backend comme un ensemble cohérent | Mappers, services CRUD, validation de rôle, verrous, gardes de suppression, chargements et tests existants | Code compilable et sécurité fonctionnelle avec la composition. Ne pas déployer les seules entités avant les services/sécurité |
| 4. Ajouter le rattachement administratif | Recherche minimale de personnes, création de profil sur identité existante, DTO spécialisés, autorisations ADMIN, conflits et transactions | Ajouter responsable à enseignant conserve personne/enseignant/compte ; doublon et personne absente correctement rejetés |
| 5. Adapter React | Formulaires de rattachement, personnes dédoublonnées dans comptes, indication des coordonnées partagées, erreurs de suppression | Ancien CRUD et nouveaux parcours utilisables ; rôle d’accès unique conservé |
| 6. Recette complète isolée | Tests JPA/API/JWT/React, calcul/PDF et suite PONDERER ; deux transactions concurrentes ; fixtures historiques non vides | Critères du §7 satisfaits sur base neuve V1→V2 et état V2 préexistant |
| 7. Bascule coordonnée future | Préparer sauvegarde restaurable et vérification sur copie ; refaire inventaire/contrôles de lecture ; arrêter tous les anciens écrivains ; déployer backend compatible puis UI ; activer le cumul après vérification | Aucun ancien processus JOINED ne lit/écrit des personnes multi-profils ; aucun changement Flyway nécessaire |

La progression peut se faire en plusieurs changements de code, mais la bascule du mapping, des services et de la validation des rôles doit être cohérente au déploiement. Après création du premier double profil, **revenir à l’ancien binaire JOINED n’est plus un retour arrière sûr**, même si le schéma SQL n’a pas changé. Ne jamais supprimer le nouveau profil ou ses liens pour rendre ce retour arrière artificiellement possible ; prévoir une correction en avant ou un retour applicatif qui garde la composition.

Les lectures avant/après bascule doivent comparer toutes les PK, relations et valeurs d’origine, pas seulement les volumes : personnes, profils, comptes et hash, liens légaux, notes, évaluations, scolarités, enseignements, bulletins, lignes et PONDERER. Ne pas exporter ces données sensibles dans le rapport ou les logs ; comparer sur l’environnement autorisé ou par empreintes techniques.

## 7. Plan de tests automatisés

### 7.1 Cadre d’exécution futur

Utiliser PostgreSQL 17 jetable, les migrations réelles V1 et V2 et `ddl-auto=validate`. Réutiliser les gardes `IsolatedPostgres` et les scripts existants ; ne pas remplacer PostgreSQL par H2 pour prouver les FK/cascades. Les tests JPA doivent utiliser `flush`, `clear`, puis recharger chaque entité par son repository. Pour vérifier le rollback ou un refus, relire dans une transaction nouvelle : une simple assertion sur le même objet Java ne suffit pas.

**Aucun test n’a été exécuté pendant cet audit**, pas même les scripts isolés, conformément à l’interdiction de démarrer Spring Boot. Les suites de contexte, d’intégration et E2E ne seront exécutées qu’au développement suivant sur l’environnement jetable.

### 7.2 Matrice de persistance et de cycle de vie

| Scénario | Assertions nécessaires après flush/clear et relecture |
|---|---|
| Personne sans profil | Persistable/lisible par `PersonneRepository` ; aucun profil créé ; coordonnées conservées |
| Profil unique, pour chacun des trois types | Un seul enregistrement personne ; PK du profil égale à la personne ; attributs spécialisés et coordonnées relus |
| Enseignant et responsable sur même personne | Les trois repositories lisent les mêmes ID, une seule personne ; chaque profil référence la même identité ; aucun polymorphisme hérité requis |
| Ajout du second profil, dans les deux ordres | Compteur de personnes et ligne du premier profil inchangés ; rôle/compte inchangés ; accès après nouveau contexte |
| Ajout sur personne seule avec compte ADMIN | Ajout réussi sans remplacement d’identité, de compte ni de hash ; ADMIN toujours actif |
| Ajout profil absent/personne inexistante/doublon | Refus approprié ; aucune identité orpheline nouvelle ; aucun profil écrasé |
| Conflit matricule ou numéro d’employé lors d’une nouvelle création | Rollback personne+profil ; anciennes données intactes |
| Mise à jour d’un profil spécialisé | Seuls ses champs spécialisés changent ; identité et autre profil conservés |
| Mise à jour de coordonnées communes | Visible via les deux profils et le mapper de compte ; matricule/numéro/profession/photo inchangés |
| Retrait indépendant de chacun des profils | Supprime seulement le profil visé, garde personne et autres profils ; vérifier aussi avec compte ADMIN et compte d’un autre rôle compatible |
| Retrait du dernier profil | Personne seule conservée, compte compatible conservé |
| Profil requis par un compte actif ou inactif | Refus 409 pour ENSEIGNANT/RESPONSABLE ; hash, rôle, activité et profils conservés |
| Références bloquantes | Inscription/scolarité/liens légaux pour élève, liens légaux pour responsable, enseignement même inactif/classe/bulletin pour enseignant : refus sans perte de lien ni `SET NULL` |
| Responsabilité légale et ses deux `@MapsId` | Clé partagée correctement remplie, noms relus par composition, suppression du lien seule sans suppression d’entité |
| Repositories d’autorisation | Exécuter tous les JPQL/chemins dérivés inventoriés ; égalités entre profils de même type et ID corrects |
| Concurrence réelle | Deux ajouts du même profil : un succès et un conflit contrôlé ; ajout/retrait/changement de rôle/affectation concurrents : aucun compte sans profil requis ni référence perdue ; vérifier absence de deadlock non géré |

Prévoir de nouveaux tests de mapping/cycle de vie, par exemple `PersonneProfilsIntegrationTest`, et les tests unitaires des services/mappers ; aucun nom de fichier nouveau n’est imposé par l’architecture.

### 7.3 Sécurité, administrateur et contrats API

Étendre `CompteUtilisateurServiceTest`, `FirstAdminBootstrapTest`, `AuthenticationServiceTest`, `CompteUtilisateurDetailsServiceTest`, `JwtServiceTest`, `ActiveAccountJwtValidatorTest`, `SecurityExpressionsTest`, `AuthorizationHttpTest` et `SecurityIntegrationTest`, ainsi que les tests de contrôleurs concernés.

- ADMIN sur personne sans profil : login et chargement JWT réussis ; bootstrap sans action quand un compte existe ; suppression/désactivation/rétrogradation du dernier ADMIN refusées ; ajout/retrait de profil autorisé sans effacer son compte.
- Rôles spécialisés : compte ENSEIGNANT exige profil enseignant, compte RESPONSABLE exige profil responsable ; un double profil est éligible à l’un ou l’autre rôle, avec un seul compte et un seul rôle choisi ; ADMIN reste indépendant.
- JWT : mêmes claims et IDs avant/après ajout de profil, ancien token encore valide si le compte ne change pas ; token rejeté après désactivation, suppression du compte, changement de sujet/rôle ; chargement réel avec personne LAZY et `open-in-view=false`.
- Double profil : avec rôle ENSEIGNANT, pas d’accès responsable supplémentaire aux notes/bulletins d’un enfant hors périmètre enseignant ; avec rôle RESPONSABLE, aucune saisie de notes/évaluations par la seule présence du profil enseignant.
- Révocation du dernier lien légal pertinent pour un compte RESPONSABLE : perte d’accès à l’élève, sa photo, ses scolarités, notes et bulletins malgré un JWT encore valide ; aucun accès à l’enfant d’un autre responsable. Les accès aux classes/enseignements/évaluations restent ceux définis par les requêtes existantes.
- Futurs endpoints de personnes/rattachement : non authentifié refusé, ENSEIGNANT/RESPONSABLE refusés, ADMIN autorisé ; aucune modification de rôle/compte/identité par champ non prévu.
- API historiques : JSON plats et champs stables, ID commun dans réponses et `Location`, POST de création classique, PUT, DELETE de profil supprimable ; 400 pour validation, 404 pour absence, 409 pour doublon/dépendance selon le contrat existant ; photo et PDF servis avec contrôle d’accès.

Tester aussi des requêtes HTTP avec de **vrais JWT signés et validation du compte** : les mocks de services ou le post-processeur `jwt()` seuls ne prouvent pas le comportement de `ActiveAccountJwtValidator`.

### 7.4 Bulletins, PONDERER et historique

Conserver et exécuter ultérieurement `BulletinCalculServiceTest`, `BulletinPdfServiceTest`, `BulletinControllerTest`, `PondererIntegrationTest` et `PondererMigrationTest`. Ajouter un scénario intégré avec élève, enseignant+responsable, comptes et responsabilités légales, données de notes et bulletins persistés non vides.

Comparer les résultats attendus avant/après ajout puis retrait d’un **profil responsable sans dépendance** : lignes par matière, notes prises en compte, normalisation, coefficients PONDERER/évaluation, moyennes exactes et arrondis identiques. Couvrir deux enseignants pour la même matière, barèmes différents, absence de notes, matière sans moyenne, enseignement hors période, pondération absente/invalide, statuts de notes existants. Pour le PDF, comparer contenu utile et identité, sans imposer l’égalité binaire si des métadonnées varient.

Une fixture de bulletin avec `id_validateur` doit rendre le retrait de son enseignant impossible et conserver la référence après refus. Comparer aussi valeurs, commentaires, dates et relations des notes/scolarités/évaluations, contenu des bulletins/lignes et liens des enseignements. Ne pas se limiter à comparer les nombres de lignes.

### 7.5 React et recette de bout en bout

Étendre [roles.test.jsx](../../frontend/src/test/roles.test.jsx) et les tests de formulaires avec Vitest/Testing Library. Conserver [ponderations.test.jsx](../../frontend/src/test/ponderations.test.jsx). Ajouter les scénarios suivants :

- Sélectionner une personne enseignante et lui ajouter un profil responsable ; l'interface affiche ses deux profils avec le même ID, sans deuxième identité.
- Vérifier les options de création de compte pour ENSEIGNANT, RESPONSABLE et ADMIN : un seul candidat par `idPersonne`, profils disponibles correctement affichés, aucune clé React dupliquée, personne déjà titulaire d'un compte exclue des candidats.
- Si la sélection des personnes sans profil est retenue, permettre leur choix pour ADMIN et refuser les rôles spécialisés sans profil correspondant.
- Modifier une coordonnée commune et vérifier sa propagation dans les deux écrans, sans modification des attributs spécialisés.
- Retirer un profil supprimable puis recharger : l'autre profil reste visible et le compte continue de fonctionner. Pour les dépendances bloquantes, afficher le 409 sans disparition locale trompeuse.
- Conserver CRUD élève, photo, listes/labels de classes, enseignements, scolarités et responsabilités légales ; même contrat de noms et d'identifiants.
- Vérifier les routes et boutons selon le rôle du compte pour une personne double profil ; aucun menu responsable supplémentaire pour son seul profil métier.
- Calculer et télécharger le bulletin autorisé, comparer les résultats attendus et vérifier le refus des données d'autrui.

Réutiliser les scénarios de [frontend/e2e](../../frontend/e2e/) : `admin-eleves.spec.js`, `admin-enseignants.spec.js`, `admin-responsables.spec.js`, `admin-classes.spec.js`, `admin-enseignements.spec.js`, `admin-scolarites.spec.js`, `admin-evaluations.spec.js`, `admin-notes.spec.js`, `admin-bulletins.spec.js` et `roles-access.spec.js`. Ajouter une recette dédiée au cumul des profils, aux comptes et aux liens légaux. Leur exécution future doit passer par l'environnement isolé, jamais l'API locale de développement existante.

### 7.6 Non-régression du schéma et des migrations

Sur des bases jetables uniquement :

1. Créer une base neuve avec V1 puis V2, puis valider les nouveaux mappings. Aucun troisième script de migration n'est attendu pour ce lot.
2. Charger une fixture V1 anonyme avec identifiants explicites et historique pédagogique/comptes, appliquer V2 inchangée, puis relire avec la composition. Comparer les projections d'origine.
3. Partir d'un état V2 déjà peuplé, ouvrir le nouveau contexte Hibernate et vérifier que l'initialisation ne modifie aucune donnée métier ni aucun objet de schéma.
4. Vérifier les empreintes de V1/V2, l'historique Flyway, les contraintes PK/FK/UNIQUE, l'absence de générateur sur les profils et la conservation du miroir/triggers PONDERER.
5. Tester explicitement les cascades et `SET NULL` sur l'instance jetable pour prouver que les garde-fous des services bloquent les opérations dangereuses ; aucun essai destructif n'est autorisé sur la base locale existante.

## 8. Décisions métier restant à prendre

| Sujet | Décision nécessaire | Recommandation pour garder le lot borné |
|---|---|---|
| Parcours de rattachement | Nouveaux endpoints dédiés ou extension des POST existants ? | Opération distincte par personne, avec DTO spécialisé ; garder les POST historiques compatibles |
| Création sans profil et sélection de personnes | Faut-il un écran complet de personnes, ou une recherche administrative minimale ? Peut-on créer une personne seule depuis l'interface ? | Recherche minimale protégée pour réutiliser une identité ; création autonome facultative. Ne pas empêcher la conservation des personnes seules existantes |
| Périmètre du recouvrement | Quelles combinaisons avec ELEVE sont utiles, et quelles règles d'âge ou d'auto-responsabilité appliquer ? | Le mapping n'impose aucune exclusion. Le cumul enseignant+responsable est acquis ; ne pas inventer de règle d'exclusion sur les autres combinaisons |
| Modification d'identité commune | Les PUT de profils peuvent-ils modifier l'identité commune, ou faut-il un parcours séparé ? | Conserver les PUT pour compatibilité et informer de l'effet partagé ; séparer ensuite si souhaité. Un rattachement ne modifie jamais l'identité |
| Suppression et archivage | Refuser, réaffecter, archiver ou anonymiser quand il existe des références ? Que conserver des liens légaux révoqués et photos ? | Refus conservateur pour toute référence ; pas d'archivage implicite ni d'effacement en cascade de données historiques |
| Profil requis par un compte | Faut-il refuser le retrait, désactiver le compte, ou changer explicitement son rôle ? | Refuser tant que le compte garde le rôle correspondant, même inactif. Aucun changement automatique de rôle, aucune suppression du compte |
| Accès aux deux fonctions | Un compte double profil doit-il pouvoir agir dans les deux fonctions simultanément ? | Garder un rôle unique et les autorisations actuelles. Toute évolution exige une décision spécifique ; ni nouvelle table de rôles ni second compte dans ce lot |
| Contrôle JWT des profils | Ajouter une vérification de profil à chaque requête, au-delà de l'invariant maintenu par les services ? | Maintenir au minimum l'invariant lors des mutations ; décider séparément du contrôle supplémentaire et de sa réponse HTTP |
| Recherche et doublons d'identité | Quels critères afficher, et qui arbitre une identité ambiguë ou deux personnes déjà créées ? | Sélection explicite par ID, critères minimaux pour ADMIN ; aucune fusion automatique par nom/email et aucune reprise de doublons dans ce lot |
| Écrivains externes | Les garde-fous applicatifs suffisent-ils, ou faut-il empêcher aussi tout DELETE SQL dangereux ? | Inventorier les écrivains. Une protection globale peut nécessiter des droits ou migrations ciblés, hors du simple changement de mapping |

Ces décisions concernent les parcours et politiques encore ouverts. Elles ne remettent pas en discussion les exigences acquises : personne sans profil autorisée, cumul enseignant+responsable avec une seule identité, identifiants existants conservés, profils distincts du rôle d'accès. La refonte du validateur de bulletin et l'habilitation ne doivent pas être anticipées dans ce lot.

**Synthèse : réalisation du lot 2 sans migration SQL confirmée pour le périmètre Personne/profils.** Les PK/FK partagées existent déjà dans V1 et sont conservées en V2. Le travail futur doit livrer ensemble composition JPA, réutilisation explicite d'identité, suppression de profil protégée, validation des rôles par existence de profil et compatibilité des API/écrans. L'acceptation finale dépend des tests Hibernate sur PostgreSQL isolé, de la préservation du compte ADMIN et du calcul des bulletins, ainsi que d'une bascule sans ancien écrivain JOINED.
