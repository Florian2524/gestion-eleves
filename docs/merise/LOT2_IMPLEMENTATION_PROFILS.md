# Lot 2 — Implémentation des profils PERSONNE

Implémentation du **27 octobre 2026**, branche **`feature/merise-lot2-profils`**, à partir de `affcc1573a33f40b412eadd2938d62d15350c4e7`. L'[audit intégral](LOT2_AUDIT_HERITAGE.md) est conservé dans cette branche. Ce document décrit l'implémentation, ses tests et la revue finale préparant la publication de la branche.

## Architecture JPA réalisée

`Personne` est une entité autonome, concrète, conservant son `id_personne` généré par `IDENTITY` et ses coordonnées. L'annotation d'héritage `JOINED` est supprimée. `Eleve`, `Enseignant` et `Responsable` sont trois entités indépendantes, sans classe mère persistante, chacune avec :

- un attribut `@Id Long idPersonne`, sans générateur ;
- une association obligatoire `@OneToOne(fetch = LAZY) @MapsId` vers `Personne` ;
- une jointure sur la colonne `id_personne` et le nom de FK existants ;
- un constructeur recevant explicitement une personne ; aucun setter permettant de déplacer un profil vers une autre personne ;
- aucune cascade JPA, aucun `orphanRemoval`, aucune référence inverse imposant un profil à la personne.

Une personne peut donc exister seule, avec un profil, ou avec plusieurs profils. Les entités spécialisées partagent l'identifiant de leur personne ; elles ne partagent plus une identité polymorphe Hibernate. Les relations pédagogiques continuent à viser les profils : une classe ou un enseignement exige un enseignant, une scolarité exige un élève. Le validateur de bulletin reste un enseignant dans ce lot.

Les mappers accèdent explicitement à `profil.getPersonne()` pour les coordonnées. Les chemins JPA `enseignant.idPersonne`, `eleve.idPersonne`, etc. restent valides : les JPQL et méthodes dérivées existants sont conservés. Les listes et lectures de profils utilisent un `EntityGraph` sur `personne`. Le chargement de l'utilisateur au login dispose d'une transaction de lecture et d'un chargement explicite de la personne. `open-in-view=false` et `ddl-auto=validate` restent inchangés.

`ProfilPersonneService` centralise la persistance explicite (`persist` personne, puis profil, puis `flush`), les verrous et les contrôles de suppression. Le rattachement utilise `persist`, jamais un `merge` de graphe fourni par le client. L'échec d'insertion d'un profil annule aussi l'insertion de sa nouvelle personne dans la même transaction.

## API réalisées et compatibilité

Les POST historiques de création `/eleves`, `/enseignants`, `/responsables` gardent leurs requêtes, réponses plates et codes de réussite. Ils créent une **nouvelle personne avec son premier profil**, même si son nom ou son email ressemble à une autre identité. Aucun rapprochement automatique n'est effectué.

Nouveaux endpoints, exclusivement ADMIN :

| Méthode et route | Requête / réponse |
|---|---|
| `GET /personnes` | Tableau d'identités minimales : `idPersonne`, `nom`, `prenom`, `emailContact`, `profils`. Les profils sont `ELEVE`, `ENSEIGNANT`, `RESPONSABLE`, ou une liste vide |
| `POST /personnes/{idPersonne}/eleve` | `matricule`, `dateNaissance` ; réponse `EleveResponse`, HTTP 201 |
| `POST /personnes/{idPersonne}/enseignant` | `numeroEmploye` ; réponse `EnseignantResponse`, HTTP 201 |
| `POST /personnes/{idPersonne}/responsable` | `profession` facultative ; réponse `ResponsableResponse`, HTTP 201 |

Chaque rattachement charge et verrouille la personne indiquée par l'URL, vérifie l'absence du profil, vérifie les valeurs uniques nécessaires, puis insère uniquement le profil. Les DTO ne portent aucun champ d'identité ou de compte à modifier. Des coordonnées supplémentaires dans un JSON ne remplacent donc pas l'identité existante. La réponse expose le même `idPersonne`, avec une `Location` vers la ressource spécialisée historique.

Un profil déjà présent produit un **409**, une personne absente un **404**, et une requête invalide un **400**. Les conflits de contrainte restent traduits en 409. Le message générique de conflit a été adapté pour ne plus parler uniquement des coefficients.

Les PUT historiques gardent leur effet sur les coordonnées communes : la personne gérée est modifiée et les autres profils reflètent ce changement. Cet effet est annoncé dans les formulaires. Les routes de photo, les contrats des classes/enseignements/scolarités/notes et le calcul/PDF des bulletins sont conservés. Aucun endpoint général de suppression de personne n'est exposé.

## Interfaces React

Les formulaires enseignants, responsables et élèves proposent explicitement :

1. **Créer une nouvelle personne**, avec les champs d'identité historiques ;
2. **Rattacher à une personne existante**, avec sélection nominative, identifiant visible et profils déjà présents, puis saisie des seuls champs spécialisés.

Les personnes possédant déjà le profil demandé sont retirées des candidats. Le formulaire de rattachement indique que l'identité et ses coordonnées seront conservées. Les écrans enseignants/responsables affichent l'identifiant et les profils cumulés. Les messages de confirmation de suppression précisent que la personne et ses autres profils restent conservés.

La page des comptes utilise la liste des personnes au lieu de concaténer les enseignants et responsables. Elle dédoublonne par `idPersonne`, affiche tous les profils disponibles et exclut les personnes ayant déjà un compte. Une personne sans profil peut être choisie pour ADMIN ; ENSEIGNANT et RESPONSABLE exigent leur profil respectif. Le rôle du compte reste un choix unique.

Aucun changement de navigation ou d'autorisation n'est déduit de la présence d'un second profil. L'élève conserve son CRUD, la recherche et les photos ; les sélecteurs de classe/enseignement restent fondés sur les identifiants de profils existants.

## Suppression et intégrité des données

| Profil visé | Suppression refusée si… |
|---|---|
| Élève | Une inscription, une scolarité ou une responsabilité légale référence ce profil |
| Enseignant | Un enseignement, même inactif, une classe référente, un bulletin avec validateur ou un compte de rôle ENSEIGNANT le référence |
| Responsable | Une responsabilité légale ou un compte de rôle RESPONSABLE le référence |

Les contrôles de compte s'appliquent aussi aux comptes inactifs. Un retrait ne change jamais automatiquement le rôle, l'activité ou le mot de passe d'un compte. Un compte ADMIN, ou un compte utilisant l'autre profil encore présent, est conservé. Retirer le dernier profil supprimable laisse une personne sans profil.

Les références SQL `SET NULL` vers enseignant et les cascades de responsabilité légale ne sont pas utilisées comme autorisation d'effacement : les services refusent ces retraits avant toute suppression. Les scolarités, notes, évaluations, enseignements et bulletins ne sont jamais effacés pour permettre un retrait.

Un verrou pessimiste sur la personne sérialise les ajouts/retraits de profils, les modifications des coordonnées et les mutations de compte. Les affectations de classe, enseignement, inscription, scolarité et responsabilités légales participent à ce protocole. Les deux personnes d'un lien légal sont verrouillées dans l'ordre croissant de leurs identifiants ; le contrôle de doublon du lien est effectué après verrouillage. Une mutation de compte relit son état après acquisition du verrou.

Les gardes du dernier ADMIN actif et leur verrou de comptes restent en place. Des tests à deux transactions vérifient les ajouts concurrents, l'affectation de classe face au retrait, le changement de rôle face au retrait, et deux désactivations concurrentes d'ADMIN.

Les références et fichiers photo sont conservés lors de cette refonte. Aucun nettoyage automatique d'identité ou de photo n'est ajouté.

## Sécurité

Les deux `instanceof` métier de `CompteUtilisateurService` sont remplacés par des recherches d'existence dans les repositories de profils. Les tests de type relatifs au principal de sécurité et aux claims numériques restent inchangés.

Les rôles **ADMIN, ENSEIGNANT, RESPONSABLE**, le compte unique par personne et le rôle unique par compte sont conservés. Aucun nouveau système de rôles, aucune seconde autorité et aucun compte supplémentaire ne sont créés à l'ajout d'un profil.

La chaîne HTTP et `@PreAuthorize` réservent `/personnes` et ses rattachements à ADMIN. La création de compte via register reste soumise à l'administrateur dans le service. Les filtres de listes, les droits par identifiant, les contrôles sur les photos et l'autorisation de consultation des bulletins sont conservés.

Les JWT gardent leurs claims et leur validation actuelle : compte actif, sujet, identifiants et rôle courant. Les tests utilisent de vrais JWT signés pour démontrer qu'un enseignant également responsable n'obtient pas automatiquement l'accès aux enfants hors de son périmètre enseignant ; qu'un compte RESPONSABLE n'obtient pas le droit de saisie par son profil enseignant ; et qu'un changement de rôle rend l'ancien JWT obsolète. La révocation du lien légal retire l'accès malgré un JWT encore valide.

Le compte ADMIN de développement n'a pas été utilisé ou modifié. Le login ADMIN sans profil, le bootstrap sans action sur un compte existant, la conservation du hash après ajout/retrait de profil et les protections du dernier administrateur sont vérifiés sur des fixtures temporaires.

## Fichiers modifiés ou créés

Chemins Java relatifs à `backend/src/main/java/fr/afpa/backend/` :

| Zone | Fichiers |
|---|---|
| Entités modifiées | `entity/Personne.java`, `entity/Eleve.java`, `entity/Enseignant.java`, `entity/Responsable.java` |
| Nouveaux DTO | `dto/personne/PersonneResponse.java`, `dto/eleve/EleveProfilRequest.java`, `dto/enseignant/EnseignantProfilRequest.java`, `dto/responsable/ResponsableProfilRequest.java` |
| Nouveau contrôleur | `controller/PersonneController.java` |
| Nouveau service | `service/ProfilPersonneService.java` |
| Mappers modifiés | `mapper/EleveMapper.java`, `mapper/EnseignantMapper.java`, `mapper/ResponsableMapper.java`, `mapper/ResponsabiliteLegaleMapper.java` |
| Repositories modifiés | `repository/PersonneRepository.java`, `EleveRepository.java`, `EnseignantRepository.java`, `ResponsableRepository.java`, `CompteUtilisateurRepository.java`, `ClasseRepository.java`, `InscriptionRepository.java`, `ScolariteRepository.java`, `ResponsabiliteLegaleRepository.java`, `BulletinRepository.java` |
| Services modifiés | `service/EleveService.java`, `EnseignantService.java`, `ResponsableService.java`, `CompteUtilisateurService.java`, `ClasseService.java`, `EnseignementService.java`, `InscriptionService.java`, `ScolariteService.java`, `ResponsabiliteLegaleService.java`, `BulletinCalculService.java` |
| Sécurité et erreurs | `config/SecurityConfig.java`, `security/CompteUtilisateurDetailsService.java`, `exception/GlobalExceptionHandler.java` |

Chemins de tests Java relatifs à `backend/src/test/java/fr/afpa/backend/` :

| Zone | Fichiers |
|---|---|
| Nouveaux tests d'intégration | `integration/PersonneProfilsIntegrationTest.java`, `integration/ProfilsConcurrenceIntegrationTest.java` |
| Fixtures/tests adaptés | `service/EleveServiceTest.java`, `EnseignantServiceTest.java`, `ResponsableServiceTest.java`, `CompteUtilisateurServiceTest.java`, `ClasseServiceTest.java`, `EnseignementServiceTest.java`, `InscriptionServiceTest.java`, `ScolariteServiceTest.java`, `ResponsabiliteLegaleServiceTest.java`, `ElevePhotoServiceTest.java`, `BulletinCalculServiceTest.java` |

Chemins React relatifs à `frontend/` :

| Zone | Fichiers |
|---|---|
| API modifiée | `src/api/schoolApi.js` |
| Composant modifié | `src/components/school/ResourcePage.jsx` |
| Nouveaux composants/utilitaires | `src/components/school/PersonneChoice.jsx`, `src/components/school/personneProfiles.js` |
| Pages modifiées | `src/pages/AdministrationPages.jsx`, `src/pages/ComptesUtilisateursPage.jsx`, `src/pages/ElevesPage.jsx` |
| Nouveaux tests | `src/test/profils.test.jsx`, `e2e/profils-personne.spec.js` |

Documentation ajoutée/conservée : `docs/merise/LOT2_AUDIT_HERITAGE.md` et le présent `docs/merise/LOT2_IMPLEMENTATION_PROFILS.md`. Les fichiers Flyway, scripts d'isolation, dépendances, configuration de production et volumes Docker de développement restent inchangés.

## Vérifications et résultats

Toutes les suites ci-dessous sont terminées avec succès. Les exécutions ont exclusivement utilisé les scripts d'isolation existants, des bases PostgreSQL 17 temporaires sur ports aléatoires et, pour Playwright, des processus backend/frontend et un répertoire photo dédiés. Aucune connexion à la base PostgreSQL locale existante n'a été nécessaire pendant cette implémentation.

| Vérification | Commande | Résultat final |
|---|---|---|
| Suite Java complète, JPA, sécurité, migrations et concurrence | `./scripts/test-ponderer.sh` | 400 tests réussis dans 45 suites, dont 39 nouveaux cas d’intégration ; aucun échec, erreur ou test ignoré |
| Tests React | `cd frontend && npm test -- --run` | 16 tests réussis, 3 fichiers |
| Lint frontend | `cd frontend && npm run lint` | Réussi, aucune erreur |
| Compilation frontend | `cd frontend && npm run build` | Réussie |
| Compilation Java/tests | `./backend/mvnw -f backend/pom.xml -DskipTests test-compile` | Réussie |
| Packaging Java et recette navigateur complète | `./scripts/test-e2e.sh` | Packaging réussi ; 15 scénarios Chromium réussis |
| Vérification du diff | `git diff --check` | Réussie |

Les nouveaux tests JPA vérifient personne seule, chaque profil, cumul dans les deux ordres, mêmes identifiants après `flush/clear`, ajout HTTP des trois profils, doublons, modifications de coordonnées partagées, suppression indépendante et conservation du compte, interdiction des retraits dangereux, responsabilités légales avec clé composée, bootstrap/login ADMIN et rôle unique. Les tests concurrents utilisent deux connexions/transactions effectives et vérifient également le rollback d'une nouvelle identité lorsque le profil échoue.

Le calcul PONDERER est comparé avant/après ajout/retrait du profil responsable d'un enseignant : mêmes lignes, moyenne attendue de 14,40, notes et pondération conservées. Les tests existants de calcul, d'arrondis, de regroupement par matière, d'absence de notes, de PDF et de migration V1→V2 restent inclus dans la suite complète.

Les nouveaux tests React couvrent création historique, rattachement sans coordonnées, affichage du cumul, conservation du formulaire après conflit et dédoublonnage des comptes avec personne sans profil pour ADMIN. La recette Playwright couvre les trois rattachements, le compte conservé et le refus de retirer le profil exigé par son rôle, en plus des parcours historiques élèves/classes/enseignements/notes/bulletins et des permissions des rôles.

La première exécution de la suite Java a révélé deux stubs unitaires de rôle à adapter ; ils ont été corrigés. Le lint a conduit à séparer l'utilitaire de libellés du composant React. La première recette navigateur a révélé un sélecteur de test ambigu entre les options des deux listes ; le scénario utilise désormais le rôle accessible et le nom exact du champ. Les résultats finaux ne confondent pas ces tentatives avec une réussite de la dernière suite.

## PostgreSQL et Flyway

**Aucune migration SQL supplémentaire n'est nécessaire pour cette composition.** Les tests démarrent le mapping réalisé avec les seules migrations V1 et V2 et `ddl-auto=validate`, insèrent et relisent les profils à PK partagée, et contrôlent l'historique Flyway, les trois FK de profils et l'absence de générateur sur leurs identifiants. Les tests PONDERER conservent les contrôles de reprise V1→V2 avec historique pédagogique non vide.

Empreintes des migrations conservées :

- V1 : `f3d0d0694f75b6c77f6261c740f2eb235102fb90d8816a4263b1295276d084ee` ;
- V2 : `35734eef70167a37c7f97749f364b88bdd10b6c7396df1c8a36e41548adb25f9`.

Aucune V3, aucun `repair`, `clean`, reset de séquence ou modification de schéma n'est introduit. Les changements de profil utilisent les données et clés déjà présentes ; aucune reprise d'identifiants ni fusion d'identités n'est requise. Les tables et leurs noms de colonnes/FK ne changent pas. La base locale, ses données, sauvegardes et volumes Docker de développement restent intacts.

## Limites et précautions de déploiement

- Le listing administratif `/personnes` reste une liste complète, cohérente avec les autres sélecteurs actuels. Il charge les profils en quatre requêtes, sans recherche par personne. Une pagination/recherche serveur sera utile si les effectifs augmentent ; aucune fusion automatique ne doit être ajoutée pour la remplacer.
- Les garde-fous de suppression protègent les chemins applicatifs. Les cascades et `SET NULL` SQL existants restent inchangés : un écrivain SQL externe peut encore les déclencher. Les écritures externes ne doivent pas contourner ce protocole.
- Les profils ne sont pas des autorisations. L'accès simultané aux deux fonctions avec un compte à plusieurs rôles reste hors périmètre ; le compte garde un seul rôle.
- Les coordonnées sont communes : les PUT existants peuvent les modifier pour tous les profils. L'interface avertit l'administrateur ; un parcours séparé d'édition d'identité pourrait être ajouté ultérieurement.
- Les responsabilités légales ne sont pas historisées par le schéma actuel. Leur révocation reste une opération distincte ; aucun archivage, anonymisation ou effacement automatique n'est ajouté.
- Les noms/photos et les bulletins calculés restent dynamiques. Aucun bulletin versionné, habilitation de validation, nouvelle règle de période ou autre lot n'est commencé.
- Avant un déploiement réel, arrêter tous les anciens écrivains utilisant JOINED. Après création d'une personne multi-profils, revenir à l'ancien binaire n'est plus sûr, même sans changement SQL. Prévoir une correction en avant et une sauvegarde restaurable selon la procédure de déploiement ; aucune sauvegarde existante n'a été manipulée ici.

**Le lot 2 est prêt pour une revue avant commit et push.** La suite Java finale compte 400 tests réussis, les tests React 16 et la recette Playwright 15 ; lint, compilations et packaging réussissent. Aucune migration SQL supplémentaire n’est nécessaire. Aucun déploiement ni démarrage contre la base locale n’a été effectué. La publication de la branche est autorisée à l’issue de la revue finale ; elle ne constitue pas une fusion dans `main`.

## Revue finale avant publication

La revue finale du 27 octobre 2026 couvre les quatre mappings, la persistance après `flush/clear`, la distinction création/rattachement, les garde-fous de retrait et leurs verrous, les comptes et JWT, les DTO et parcours React, ainsi que le calcul PONDERER. Aucun problème fonctionnel bloquant n'a été relevé. Les corrections de cette revue concernent les mentions documentaires de publication et une ligne vide superflue en fin de fichier JavaScript, sans modification fonctionnelle. Le contrôle final inclut les nouveaux fichiers indexés avec `git diff --cached --check`.

Les limites décrites restent explicites : listing administratif sans pagination, garde-fous applicatifs ne remplaçant pas les restrictions SQL pour un écrivain externe, et nécessité d'arrêter les anciens écrivains JOINED avant utilisation de profils cumulés. Elles n'empêchent pas l'ouverture d'une Pull Request pour ce lot.

La sélection des fichiers à publier est limitée aux sources Java/React, tests synthétiques et aux deux documents du lot. Les secrets et paramètres d'environnement locaux, sauvegardes, volumes, builds, captures/rapports temporaires et données de développement sont exclus. V1 et V2 sont comparées au commit de départ, octet pour octet, et aucune V3 n'est présente.

La réexécution locale de revue confirme 400 tests Java, 16 tests React et 15 scénarios Playwright réussis, ainsi que le lint, les compilations et le packaging, exclusivement sur infrastructure isolée.

La revue autorise un commit sur `feature/merise-lot2-profils`, avec le message demandé, puis le push de cette seule branche. La CI GitHub Actions déclenchée par ce push doit être contrôlée sur le SHA publié ; son résultat sera communiqué avec le lien de la branche. Aucune fusion ni aucun déploiement ne fait partie de cette publication.
