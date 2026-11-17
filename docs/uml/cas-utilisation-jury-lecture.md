# Cas d'utilisation jury — audit et première proposition graphique

Proposition à examiner visuellement avant validation. Référence du code local :
commit `03c6d55`. L'audit porte sur les fonctionnalités et autorisations existantes ;
il ne modifie pas leur fonctionnement.

## 1. Livrables et lecture

Le modèle complet est réparti en **trois vues complémentaires**, dans un même
fichier draw.io et un **PDF de trois pages A3 paysage** :

1. **Vue générale — accès et consultation** : acteurs, généralisation, accès public
   et usages communs aux trois rôles, avec leurs périmètres.
2. **Administration scolaire** : opérations réservées à l'administrateur.
3. **Saisie pédagogique — évaluations et notes** : opérations d'écriture communes
   à l'administrateur et à l'enseignant, avec leurs restrictions respectives.

La première page seule ne prétend donc pas montrer toutes les opérations
d'administration. Les trois pages constituent ensemble le modèle documenté.

| Fichier | Contenu |
|---|---|
| [cas-utilisation-jury.drawio](cas-utilisation-jury.drawio) | Source native éditable, trois onglets |
| [cas-utilisation-jury.pdf](cas-utilisation-jury.pdf) | Les trois vues réunies, vectoriel A3 paysage |
| [cas-utilisation-jury.svg](cas-utilisation-jury.svg) / [PNG](cas-utilisation-jury.png) | Vue générale, page 1 |
| [cas-utilisation-jury-administration.svg](cas-utilisation-jury-administration.svg) / [PNG](cas-utilisation-jury-administration.png) | Administration, page 2 |
| [cas-utilisation-jury-pedagogie.svg](cas-utilisation-jury-pedagogie.svg) / [PNG](cas-utilisation-jury-pedagogie.png) | Saisie pédagogique, page 3 |

Ouvrir le PDF pour parcourir tout le modèle. Ouvrir les SVG dans un navigateur
pour zoomer. Dans draw.io/diagrams.net : **Fichier → Ouvrir depuis → Appareil**,
puis choisir les onglets en bas. Les ellipses, silhouettes, textes, frontières et
connecteurs sont de vrais éléments éditables ; aucune image n'est incorporée.

Les SVG ont des dimensions explicites de 420 × 297 mm. Chaque PNG mesure
5 040 × 3 564 pixels, soit environ 305 ppp à cette taille. Imprimer le PDF sur A3
paysage en taille réelle. La même palette sobre que le diagramme de classes est
reprise : accès en bleu, personnes en vert, parcours en ocre, organisation en
violet et résultats en rose.

## 2. Diagrammes retrouvés et défauts constatés

### Source courante

La source modifiable existante est [cas-utilisation.puml](cas-utilisation.puml),
accompagnée de [cas-utilisation.png](cas-utilisation.png). Elle contient **13 cas,
trois acteurs concrets, 16 associations acteur–cas et un `extend`**. Elle ne contient
aucune généralisation d'acteurs et aucun acteur Professeur principal.

Les acteurs y sont déjà à l'extérieur de la frontière : ce point est conservé,
sans présenter la refonte comme la correction d'une erreur inexistante.

Les principaux écarts avec le code sont :

- L'administrateur n'est pas associé à la gestion des évaluations et des notes,
  alors que ces écritures sont autorisées par le backend et l'interface.
- Les inscriptions sont présentées comme pouvant affecter une classe : cette
  opération relève en réalité des scolarités, dans un autre module.
- Les comptes, les profils enseignants et les profils responsables sont mélangés
  dans un même cas « utilisateurs et rôles ».
- Les pondérations classe–matière n'apparaissent pas explicitement.
- La lecture des enseignements et des évaluations par les trois rôles manque de
  visibilité ; les cas de lecture des élèves et notes sont répartis entre acteurs
  alors qu'ils partagent des objectifs sous périmètres différents.
- Le calcul effectif du bulletin à la demande n'est pas explicite.

Le rendu courant est très vertical, avec des libellés longs, des liaisons de grande
longueur, des faisceaux et des notes éloignées. Son fond transparent peut également
être affiché sombre selon le visualiseur.

### Export historique distinct

[cas-utilisation.png.png](cas-utilisation.png.png) représente une version plus
ancienne, avec Utilisateur, Professeur principal, appréciations générale et matière,
et validation de bulletin. Aucune source séparée correspondant exactement à cet
ancien export n'a été retrouvée parmi les sources UML présentes.

La spécialisation « Professeur principal est un Enseignant » est compréhensible
sur le plan métier. Elle ne justifie cependant pas de lui attribuer des actions
que l'application actuelle n'implémente pas. **Rédiger l'appréciation générale,
rédiger l'appréciation de matière et valider un bulletin ne sont pas repris** :
aucun parcours frontend ni endpoint de gestion de ces actions n'est exposé.
La présence de champs correspondants dans les entités ne constitue pas une
fonctionnalité utilisable.

Les deux PNG et le PlantUML d'origine restent intacts.

## 3. Acteurs et autorisations réelles

### Rôles de sécurité

[RoleUtilisateur.java](../../backend/src/main/java/fr/afpa/backend/entity/RoleUtilisateur.java)
contient exactement `ADMIN`, `ENSEIGNANT`, `RESPONSABLE`.

| Acteur | Capacités retenues | Périmètre |
|---|---|---|
| Administrateur | Toutes les consultations, administration scolaire, gestion des évaluations et notes | Ensemble des données, sous réserve des règles métier et contraintes de suppression |
| Enseignant | Consultations ; création, modification et suppression d'évaluations et notes | Dossiers/classes/scolarités de ses classes enseignées ; enseignements propres ; évaluations et notes de ses enseignements |
| Responsable légal | Consultations et téléchargement PDF ; aucune écriture scolaire | Élèves auxquels il est lié par une responsabilité légale, et données scolaires correspondantes |
| Utilisateur non authentifié | Accueil public et connexion | Pas de consultation scolaire ni de création publique de compte |

Sources principales :

- [SecurityConfig.java](../../backend/src/main/java/fr/afpa/backend/config/SecurityConfig.java),
  règles HTTP, notamment GET partagés et écritures évaluations/notes pour ADMIN et
  ENSEIGNANT ; les autres écritures métier sont réservées à ADMIN.
- [SecurityExpressions.java](../../backend/src/main/java/fr/afpa/backend/security/SecurityExpressions.java),
  propriété des enseignements, évaluations et notes ; rattachements aux élèves,
  scolarités et classes.
- Les contrôleurs de lecture filtrent les listes et utilisent `@PostAuthorize`
  sur les ressources individuelles :
  [EleveController](../../backend/src/main/java/fr/afpa/backend/controller/EleveController.java),
  [ClasseController](../../backend/src/main/java/fr/afpa/backend/controller/ClasseController.java),
  [ScolariteController](../../backend/src/main/java/fr/afpa/backend/controller/ScolariteController.java),
  [EnseignementController](../../backend/src/main/java/fr/afpa/backend/controller/EnseignementController.java),
  [EvaluationController](../../backend/src/main/java/fr/afpa/backend/controller/EvaluationController.java),
  [NoteController](../../backend/src/main/java/fr/afpa/backend/controller/NoteController.java).

Les autorisations ne sont donc pas déduites uniquement de la visibilité d'un bouton
ou des règles globales HTTP. Les contrôles de propriété et les filtres des listes
font partie du modèle d'accès observé.

### Professeur principal

Le professeur principal est renseigné par l'administrateur dans le formulaire de
classe. Le code ne définit ni rôle JWT `PROFESSEUR_PRINCIPAL`, ni permission de
validation de bulletin liée à cette affectation.

La lecture des classes par un enseignant dépend des **enseignements**, et non de
la seule affectation comme professeur principal :
[ClasseRepository.java](../../backend/src/main/java/fr/afpa/backend/repository/ClasseRepository.java)
et [EleveRepository.java](../../backend/src/main/java/fr/afpa/backend/repository/EleveRepository.java).
Le professeur principal n'est donc pas un acteur spécialisé du nouveau diagramme.
Son affectation est documentée dans **A08 Organiser les classes**.

Sources complémentaires :
[ClasseService.java](../../backend/src/main/java/fr/afpa/backend/service/ClasseService.java),
[AdministrationPages.jsx](../../frontend/src/pages/AdministrationPages.jsx).

### Authentification, comptes et session

`POST /auth/register` est déclaré `permitAll` au niveau du filtre HTTP, mais
[AuthenticationService.register](../../backend/src/main/java/fr/afpa/backend/service/AuthenticationService.java)
exige une authentification et le rôle ADMIN. Il ne faut donc pas inventer un cas
« S'inscrire » accessible au public. La création de compte est couverte par A04.

Un compte porte un seul rôle. Le rôle ENSEIGNANT exige un profil enseignant et
RESPONSABLE un profil responsable, contrôlés par
[CompteUtilisateurService.java](../../backend/src/main/java/fr/afpa/backend/service/CompteUtilisateurService.java).
Les profils multiples d'une personne ne donnent pas plusieurs rôles simultanés.

La déconnexion existe dans
[AuthContext.jsx](../../frontend/src/context/AuthContext.jsx) et
[ConnectedHeader.jsx](../../frontend/src/components/layout/ConnectedHeader.jsx).
Elle efface la session locale et redirige vers la connexion. Le cas C04 ne prétend
pas révoquer un JWT côté serveur ; le backend désactive le mécanisme de logout
Spring et reste sans état.

### Routes frontend et lectures de contexte

[App.jsx](../../frontend/src/App.jsx) et
[ProtectedRoute.jsx](../../frontend/src/components/auth/ProtectedRoute.jsx) distinguent :

- `/` et `/connexion` publics ; `/espace`, `/eleves`, `/notes`, `/bulletins`,
  `/enseignements`, `/evaluations` accessibles aux trois rôles après connexion.
- `/classes`, `/scolarites`, `/inscriptions`, `/ponderations`, `/enseignants`,
  `/responsables`, `/responsabilites-legales`, `/comptes-utilisateurs`, `/matieres`
  et `/periodes` réservés à ADMIN dans la navigation.

Les enseignants et responsables ne disposent pas pour autant d'un écran complet
de gestion des classes ou scolarités. Leurs GET autorisés sont utilisés notamment
pour le contexte et les sélections du suivi scolaire. Les matières et périodes
sont des référentiels de lecture partagés ; les listes de classes et scolarités
sont filtrées. Ces lectures sont couvertes par les parcours C05 à C10 et ne sont
pas présentées comme des pages de gestion ouvertes à tous.

[AdministrationPages.jsx](../../frontend/src/pages/AdministrationPages.jsx) et
[ResourcePage.jsx](../../frontend/src/components/school/ResourcePage.jsx) réservent
l'écriture des enseignements à ADMIN. L'écriture des évaluations et notes est
proposée à ADMIN et ENSEIGNANT, jamais à RESPONSABLE :
[NotesPage.jsx](../../frontend/src/pages/NotesPage.jsx).

## 4. Catalogue des 24 cas représentés

Les identifiants ci-dessous servent à la traçabilité et aux cellules du draw.io.
Ils ne surchargent pas les libellés des ellipses.

### Vue 1 — accès et consultation

| ID | Cas | Acteur / portée | Preuve principale |
|---|---|---|---|
| C01 | Consulter l'accueil | Public, également accessible aux comptes connectés | `App.jsx` route `/`, `HomePage.jsx` |
| C02 | Se connecter | Utilisateur disposant d'identifiants valides | `AuthenticationController`, `AuthenticationService.login`, `LoginPage.jsx` |
| C03 | Ouvrir son espace personnel | Trois rôles authentifiés | `App.jsx` route `/espace`, `DashboardPage.jsx` |
| C04 | Se déconnecter | Trois rôles authentifiés | `AuthContext.jsx`, `ConnectedHeader.jsx` |
| C05 | Consulter les dossiers élèves | Trois rôles, selon rattachement ; photo et contexte scolaire autorisés | `EleveController`, `ClasseController`, `ScolariteController`, `ElevesPage.jsx` |
| C06 | Consulter les enseignements | Trois rôles, enseignements propres ou liés | `EnseignementController`, `SecurityExpressions`, `AdministrationPages.jsx` |
| C07 | Consulter les évaluations | Trois rôles, selon périmètre | `EvaluationController`, `SecurityExpressions`, `AdministrationPages.jsx` |
| C08 | Consulter les notes | Trois rôles, selon périmètre | `NoteController`, `SecurityExpressions`, `NotesPage.jsx` |
| C09 | Calculer et consulter un bulletin | Trois rôles, scolarité autorisée et période sélectionnée | `BulletinController`, `BulletinCalculService`, `BulletinsPage.jsx` |
| C10 | Télécharger le bulletin PDF | Même périmètre de scolarité | `BulletinController`, `BulletinPdfService`, `BulletinsPage.jsx` |

### Vue 2 — administration scolaire

Tous les cas A01 à A12 sont réservés à ADMIN.

| ID | Cas | Opérations couvertes et preuve |
|---|---|---|
| A01 | Gérer les dossiers élèves | Créer/rattacher le profil élève, modifier, retirer ; identité, coordonnées, photo. `EleveController`, `PersonneController`, `ElevesPage.jsx` |
| A02 | Gérer les profils enseignants | Créer, rattacher à une personne, modifier ou retirer le profil. `EnseignantController`, `PersonneController`, `AdministrationPages.jsx` |
| A03 | Gérer les profils responsables | Même cycle pour le profil responsable. `ResponsableController`, `PersonneController`, `AdministrationPages.jsx` |
| A04 | Administrer les comptes | Créer, modifier, activer/désactiver, changer rôle ou mot de passe, supprimer selon protections métier. `CompteUtilisateurController`, `CompteUtilisateurService`, `ComptesUtilisateursPage.jsx` |
| A05 | Lier responsables et élèves | Consulter, ajouter et retirer les liens. `ResponsabiliteLegaleController`, `ResponsabilitesLegalesPage.jsx` |
| A06 | Gérer les inscriptions | Dates et statut administratifs, sans lien direct vers Classe. `InscriptionController`, formulaire `inscriptions` |
| A07 | Affecter les élèves aux classes | Créer, consulter, modifier et supprimer des scolarités datées. `ScolariteController`, formulaire `scolarites` |
| A08 | Organiser les classes | CRUD, nom/niveau/année, professeur principal facultatif. `ClasseController`, `ClasseService`, formulaire `classes` |
| A09 | Gérer les matières | CRUD du code et du nom. `MatiereController`, formulaire `matieres` |
| A10 | Configurer les périodes | CRUD des périodes, dates, année, statut et fenêtres de saisie. `PeriodeController`, formulaire `periodes` |
| A11 | Définir les pondérations | CRUD du coefficient classe–matière. `PondererController`, `PondererService`, formulaire `ponderations` |
| A12 | Affecter les enseignements | CRUD enseignant/classe/matière, dates, activation ; coefficient compatible avec Ponderer. `EnseignementController`, `EnseignementService`, formulaire `enseignementsAdmin` |

Les contrôleurs cités se trouvent dans
[backend/controller](../../backend/src/main/java/fr/afpa/backend/controller/),
les services dans [backend/service](../../backend/src/main/java/fr/afpa/backend/service/).
Les formulaires génériques se trouvent dans
[AdministrationPages.jsx](../../frontend/src/pages/AdministrationPages.jsx).
Configurer une fenêtre de saisie n'est pas représenté comme un mécanisme de
verrouillage automatique des notes, ni comme un workflow de validation de bulletin.

### Vue 3 — saisie pédagogique

| ID | Cas | Acteurs / preuves |
|---|---|---|
| P01 | Gérer les évaluations | ADMIN : toutes ; ENSEIGNANT : celles de ses enseignements. `EvaluationController` : `@PreAuthorize` sur POST, PUT et DELETE ; `AdministrationPages.jsx` |
| P02 | Gérer les notes | ADMIN : toutes ; ENSEIGNANT : notes des évaluations de ses enseignements. `NoteController` : propriété de la note et de l'évaluation ; `NotesPage.jsx` |

Le commentaire d'une note est couvert par P02. Il ne doit pas être confondu avec
une appréciation de bulletin. Les règles de cohérence scolarité/classe et barème
de [NoteService.java](../../backend/src/main/java/fr/afpa/backend/service/NoteService.java)
restent applicables ; le diagramme n'accorde pas le droit de contourner ces règles.

## 5. Choix UML

### Associations et acteurs

Les associations acteur–cas sont des traits pleins sans flèche de séquence.
Elles expriment la participation, pas l'ordre d'exécution ni un appel HTTP.
Les acteurs sont tous à l'extérieur de la frontière du système.

Les trois rôles concrets se spécialisent vers **Utilisateur authentifié**, un
acteur abstrait représentant seulement les huit objectifs communs. Le triangle
vide pointe vers cet acteur général. Sa notation rectangulaire `«actor»` reste
une représentation d'acteur UML ; il ne s'agit ni d'une entité ni d'un composant
interne à l'application. Les différences de périmètre sont affichées à droite.

Cette abstraction ne crée aucun rôle de sécurité supplémentaire. Administrateur
n'hérite pas d'Enseignant et Responsable n'hérite d'aucune permission d'écriture.
Un même acteur retrouvé dans plusieurs onglets désigne le même rôle du modèle.

L'utilisateur non authentifié représente la situation d'accès public/connexion.
Il n'a pas d'association vers les données scolaires. C01 reste public : son
association au visiteur ne signifie pas qu'un utilisateur connecté ne peut plus
ouvrir l'accueil.

### Aucun include/extend ajouté artificiellement

L'authentification est une précondition des opérations protégées. Elle n'est pas
un `include` répété : chaque consultation ne réexécute pas le scénario de connexion.

L'ancien `PDF → consultation de bulletin` avait le bon sens pour un `extend` et
pouvait se défendre pour le bouton optionnel affiché après calcul dans le frontend.
Il n'est pas retenu dans cette vue de l'application complète : l'API PDF est une
opération autorisée indépendamment d'une consultation JSON préalable. Le nouveau
modèle relie donc explicitement l'acteur aux deux objectifs C09 et C10. Un calcul
interne commun ne justifie pas d'inventer un `include` vers un cas comprenant
aussi l'affichage de l'écran.

Résultat : **26 associations acteur–cas, trois généralisations, zéro include,
zéro extend**. Les deux liens de saisie ADMIN qui manquaient à l'ancien diagramme
sont présents dans la vue 3.

## 6. Correspondance explicite avec les 13 cas de la source courante

| Ancien cas | Nouveau(x) cas | Changement et justification |
|---|---|---|
| Se connecter à l'application | C02 | Libellé raccourci, acteur en situation non authentifiée |
| Gérer les élèves | A01 + C05 | Écriture ADMIN séparée de la consultation commune ; photo conservée |
| Gérer les inscriptions, dont affecter une classe | A06 + A07 | Séparation inscription administrative / scolarité réelle |
| Gérer les référentiels scolaires | A08, A09, A10, A12 | Décomposition des quatre modules existants |
| Gérer les utilisateurs et les rôles | A02, A03, A04 | Profils métier distingués des comptes et rôles de sécurité |
| Gérer les liens responsable–élève | A05 | Reformulation avec un verbe court ; ajout/retrait conservés |
| Consulter les élèves et classes affectés | C05, avec contexte de classe, et C06 | Périmètre enseignant conservé ; ne promet pas une page de gestion des classes |
| Gérer les évaluations | P01 + C07 | Écriture explicitée pour ADMIN et ENSEIGNANT ; lecture distincte |
| Saisir les notes | P02 + C08 | « Gérer » couvre saisie, correction, commentaire et suppression ; lecture distincte |
| Consulter les notes des élèves rattachés | C08 | Fusion de l'objectif de lecture, périmètre responsable conservé |
| Consulter les dossiers des élèves rattachés | C05 | Même fusion de l'objectif, sans élargissement du périmètre |
| Consulter un bulletin par élève et période | C09 | Calcul à la demande rendu explicite, scolarité et période conservées |
| Télécharger le bulletin PDF | C10 | Fonction conservée ; extend remplacé par une participation directe |

**Ajouts explicites par rapport aux ellipses anciennes**, sans développement de
fonctionnalité : C01 accueil public, C03 espace personnel, C04 déconnexion, C06
lecture des enseignements, C07 lecture des évaluations et A11 pondérations. Ils
correspondent à des routes, écrans et endpoints déjà présents. Les autres cas
supplémentaires proviennent de décompositions ou de séparations lecture/écriture.

Aucun objectif fonctionnel réel des 13 cas courants n'est supprimé. Par rapport
au PNG historique, les trois actions d'appréciation/validation et l'acteur spécialisé
Professeur principal sont écartés pour absence de parcours implémenté, comme indiqué
dans l'audit ci-dessus. L'affectation du professeur principal reste dans A08.

## 7. Contrôle visuel, comparaison et limites

Les deux anciens PNG ont été réellement examinés. Les nouvelles vues ont été
rendues puis inspectées en vue complète et sur des détails : généralisation,
consultations, opérations d'administration et écritures pédagogiques.

Les ajustements ont porté sur l'espacement du premier cas d'administration, la
position de son acteur pour qu'une liaison ne frôle plus son nom, une annotation
de compte répartie sur trois lignes, et le cadrage des aperçus. Les exports ont été
régénérés après ces corrections.

Contrôles finaux :

- 24 cas distincts dans les trois onglets, mêmes libellés et références de
  connecteurs entre le modèle de génération, le SVG et le draw.io.
- **Zéro croisement de relations détecté** dans les trois vues ; les points communs
  de participation au même acteur ne sont pas comptés comme croisements.
- Aucun chevauchement de textes, aucune association traversant un texte, avec
  deux unités de marge autour des boîtes de texte mesurées dans Chromium.
- Cas à l'intérieur de la frontière ; acteurs à l'extérieur ; textes d'ellipses
  contenus dans leurs boîtes et aucun texte tronqué par le canevas.
- Aucune image incorporée au draw.io. Trois pages dans le PDF, dimensions A3
  paysage à l'arrondi du moteur d'impression près, aucune image raster incorporée.
- SVG de 420 × 297 mm ; PNG de 5 040 × 3 564 pixels pour chaque vue.
- Sur A3, libellés des cas d'environ 12,8 à 15,9 points ; annotations d'au moins
  9,6 points. La taille des libellés ne dépend plus de longues listes CRUD dans
  les ellipses.

Le compromis est de passer d'une page dense à trois pages. La vue d'administration
reste celle qui possède le plus d'associations : douze traits convergent sur le
même acteur, mais leurs parcours sont directs, sans coude, sans tronc partagé et
sans croisement. Ces éventails restent à apprécier ensemble avant validation.
La troisième page est volontairement aérée ; une réduction papier en A4 n'a pas
été vérifiée.

L'inspection visuelle porte sur les SVG rendus et les PNG ; le PDF a été produit
depuis les mêmes rendus vectoriels et contrôlé structurellement. Le draw.io a été
contrôlé structurellement, sans ouverture dans son éditeur natif pendant cette
génération. Aucune impression papier n'a été effectuée. Le nombre de croisements
des anciens PNG n'a pas été mesuré automatiquement : aucune comparaison chiffrée
non vérifiée n'est annoncée pour ces images.

Comparaison SHA-256 : tous les fichiers préexistants sous `docs`, `backend/src` et
`frontend/src` sont inchangés, notamment les diagrammes de classes validés, leurs
sources et exports, et `docs/merise/LoopingImage.jpg`. Aucun code, permission,
migration, base de données, configuration ou démonstration temporaire modifié ;
aucune installation système, aucun Docker, test E2E, commit ou push exécuté.
