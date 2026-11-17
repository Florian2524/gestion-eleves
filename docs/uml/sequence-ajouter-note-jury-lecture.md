# Ajouter une note — audit et choix de modélisation

Première proposition graphique, à examiner avant validation. Le code de référence
est celui du projet local au commit `03c6d55`. Le périmètre est **la création** :
`editing = null`, et non la modification ou la suppression d'une note.

## Livrables et ouverture

| Fichier | Usage |
|---|---|
| [sequence-ajouter-note-jury.puml](sequence-ajouter-note-jury.puml) | Source PlantUML modifiable contenant deux diagrammes nommés |
| [sequence-ajouter-note-jury.pdf](sequence-ajouter-note-jury.pdf) | Deux pages vectorielles A3 paysage : nominal, puis échecs |
| [sequence-ajouter-note-jury.svg](sequence-ajouter-note-jury.svg) / [PNG](sequence-ajouter-note-jury.png) | Scénario nominal |
| [sequence-ajouter-note-jury-erreurs.svg](sequence-ajouter-note-jury-erreurs.svg) / [PNG](sequence-ajouter-note-jury-erreurs.png) | Principaux chemins d'échec |

Ouvrir le PDF pour suivre les deux vues. Les SVG permettent de zoomer sans perte.
Les PNG ont un canevas de 5 040 × 3 564 pixels. Le PDF est destiné à une impression
A3 paysage en taille réelle ; une réduction A4 n'a pas été vérifiée.

La source se prévisualise avec PlantUML, notamment dans un éditeur compatible.
Les deux blocs `@startuml` portent leurs noms de sortie : une compilation de cette
source produit les deux SVG. Exemple avec un jar PlantUML disponible :

```sh
java -Djava.awt.headless=true -jar plantuml.jar -checkonly docs/uml/sequence-ajouter-note-jury.puml
java -Djava.awt.headless=true -jar plantuml.jar -tsvg docs/uml/sequence-ajouter-note-jury.puml
```

Les PNG et le PDF fournis ont été exportés depuis ces SVG avec Chromium local,
avec conservation des proportions et, pour le PDF, une marge de 5 mm. Aucun
placement manuel ne modifie les messages produits par PlantUML.

## 1. Diagramme existant

L'original est conservé : [sequence-ajouter-note.puml](sequence-ajouter-note.puml)
et [sequence-ajouter-note.png](sequence-ajouter-note.png). Les deux ont été examinés.

Il représente déjà le POST, le contrôle de propriété, le doublon et la cohérence
classe/barème. Sa refonte précise les points suivants :

- L'acteur peut être **ADMIN ou ENSEIGNANT**, pas uniquement Enseignant.
- Le formulaire, la préparation du JSON et le traitement réel de la réponse ne
  sont pas visibles dans l'original.
- Le chemin HTTP du navigateur et celui du contrôleur sont distingués.
- Le filtre JWT/rôle, la validation des arguments par MVC et le contrôle de méthode
  `@PreAuthorize` sont distingués dans leur ordre.
- Les chargements passent explicitement par **ScolariteRepository**, puis
  **EvaluationRepository** ; NoteService ne dialogue pas directement avec PostgreSQL.
- Les erreurs ne repartent pas directement de NoteService vers React : les
  exceptions remontent à MVC, puis au gestionnaire approprié.
- Le 404, le retour `Location`, la remise à zéro du formulaire et le rechargement
  effectif des données sont documentés.

L'original imbrique les variantes autour du scénario nominal. La nouvelle source
sépare le succès et les échecs pour limiter cette imbrication et garder les étapes
de création faciles à suivre.

## 2. Parcours frontend réellement implémenté

Sources :
[NotesPage.jsx](../../frontend/src/pages/NotesPage.jsx),
[notesApi.js](../../frontend/src/api/notesApi.js),
[http.js](../../frontend/src/api/http.js),
[App.jsx](../../frontend/src/App.jsx),
[ProtectedRoute.jsx](../../frontend/src/components/auth/ProtectedRoute.jsx).

### Chargement et sélection

La page protégée est accessible aux trois rôles. Le formulaire d'écriture est
affiché lorsque le rôle n'est pas RESPONSABLE ; combiné à la protection de route,
cela donne ADMIN et ENSEIGNANT.

Un `useEffect` dépendant de `reloadKey` charge huit ressources avec `Promise.all` :

1. `GET /notes` ;
2. `GET /evaluations` ;
3. `GET /scolarites` ;
4. `GET /eleves` ;
5. `GET /enseignements` ;
6. `GET /classes` ;
7. `GET /matieres` ;
8. `GET /periodes`.

Le préfixe de base de l'API est ajouté par le client HTTP à chacune de ces requêtes.
Le diagramme commence une fois ces données chargées. Il ne les présente pas comme
huit appels séquentiels ni comme un endpoint de chargement groupé.

L'évaluation sélectionnée permet de retrouver l'enseignement, puis son `idClasse`.
La liste des scolarités proposées est filtrée localement sur cette classe. Changer
l'évaluation remet à vide une scolarité devenue incompatible ; cette sélection
n'ajoute pas d'appel API.

### Soumission et contrôles locaux

`save(event)` empêche la soumission HTML standard, puis contrôle :

- la présence d'une évaluation et d'une scolarité compatible avec sa classe ;
- une valeur non vide, finie et comprise entre zéro et le barème de l'évaluation ;
- un statut non vide après `trim()`.

Les contraintes HTML (`required`, `min`, `max`, `step`, `maxLength`) complètent ces
contrôles et peuvent empêcher le déclenchement de `save` en amont. La variante
« erreur locale, aucun POST » regroupe ce refus côté interface.

Après les contrôles, `busy` passe à true ; les précédents messages sont effacés.
Le corps envoyé est le suivant :

| Champ | Préparation côté React | Contrat de `NoteRequest` |
|---|---|---|
| `idEvaluation` | `Number(form.idEvaluation)` | Long obligatoire, positif |
| `idScolarite` | `Number(form.idScolarite)` | Long obligatoire, positif |
| `valeur` | `Number(form.valeur)` | BigDecimal obligatoire, positif ou nul, au plus trois chiffres entiers et deux décimales |
| `commentaire` | `trim()` ; chaîne vide transformée en null | String facultative |
| `statutNote` | `trim()` ; valeur initiale `SAISIE` | String non blanche, longueur maximale 30 ; pas une énumération |

Le client n'envoie ni `idEleve`, ni `idClasse`, ni `idNote`, ni `dateSaisie`.
En mode création, il appelle `createNote(body)` ; `updateNote` concerne l'autre
branche de l'interface et ne figure pas dans le scénario.

### Appel HTTP exact

`createNote` délègue à `apiRequest('/notes', { method: 'POST', body })`.
Le client utilise `VITE_API_URL` lorsqu'il est défini, sinon `/api`, sans slash final.
Le corps est sérialisé en JSON et les en-têtes comprennent `Content-Type` et
`Accept: application/json`, ainsi que `Authorization: Bearer <token>` si un jeton
est stocké.

Par défaut, le navigateur appelle donc **`POST /api/notes`**. Le proxy défini dans
[vite.config.js](../../frontend/vite.config.js) retire `/api`, et Spring reçoit
**`POST /notes`**. La flèche du diagramme utilise ce parcours fourni par défaut ;
elle ne suppose pas que toutes les configurations de déploiement utilisent la
même URL de base. Aucun `/notes/create` n'est inventé.

## 3. Autorisations et entrée dans le contrôleur

Sources :
[SecurityConfig.java](../../backend/src/main/java/fr/afpa/backend/config/SecurityConfig.java),
[SecurityExpressions.java](../../backend/src/main/java/fr/afpa/backend/security/SecurityExpressions.java),
[NoteController.java](../../backend/src/main/java/fr/afpa/backend/controller/NoteController.java),
[NoteRequest.java](../../backend/src/main/java/fr/afpa/backend/dto/note/NoteRequest.java).

L'ordre représenté est :

1. La chaîne Spring Security contrôle le JWT et autorise POST `/notes` à ADMIN ou
   ENSEIGNANT. Un jeton absent/invalide mène à 401 ; un rôle non autorisé à 403.
2. MVC désérialise les arguments et applique `@Valid NoteRequest`. Les contraintes
   de Bean Validation échouent avant l'exécution de la méthode du contrôleur.
3. L'intercepteur de méthode applique :

   ```java
   hasRole('ADMIN') or
   @securityExpressions.estProprietaireEvaluation(#request.idEvaluation(), authentication)
   ```

4. Le corps de `NoteController.create` appelle `noteService.create(request)`.

ADMIN passe par le premier terme du `or`, sans contrôle de propriété. Pour
ENSEIGNANT, `estProprietaireEvaluation` utilise son `idPersonne` et appelle :

```java
evaluationRepository.existsByIdEvaluationAndEnseignement_Enseignant_IdPersonne(
    idEvaluation, idPersonne
)
```

Le fragment `opt` du nominal représente uniquement ce contrôle supplémentaire
pour ENSEIGNANT. L'accès JPA interne à cette expression n'est pas redessiné : il
est regroupé avec la sécurité pour garder la vue centrée sur l'enregistrement.
La ligne de vie Spring Security/MVC est une abstraction des mécanismes existants,
pas une nouvelle classe du projet. Elle inclut la résolution des arguments, le
contrôle de méthode et l'appel de `SecurityExpressions`.

Avec un identifiant d'évaluation valide au sens du DTO mais inexistant, ENSEIGNANT
échoue déjà au contrôle de propriété : **403 avant le service**, et non toujours
404. Le 404 de l'évaluation dans le service concerne notamment ADMIN, ou une
disparition entre autorisation et chargement.

## 4. Ordre exact des traitements métier

Sources :
[NoteService.java](../../backend/src/main/java/fr/afpa/backend/service/NoteService.java),
[NoteRepository.java](../../backend/src/main/java/fr/afpa/backend/repository/NoteRepository.java),
[ScolariteRepository.java](../../backend/src/main/java/fr/afpa/backend/repository/ScolariteRepository.java),
[EvaluationRepository.java](../../backend/src/main/java/fr/afpa/backend/repository/EvaluationRepository.java),
[NoteMapper.java](../../backend/src/main/java/fr/afpa/backend/mapper/NoteMapper.java).

`NoteService.create` est transactionnel et exécute dans cet ordre :

| Ordre | Interaction ou traitement | Refus possible |
|---:|---|---|
| 1 | `NoteRepository.existsByScolarite_IdScolariteAndEvaluation_IdEvaluation(idScolarite, idEvaluation)` | `DuplicateResourceException` si true → 409 |
| 2 | `ScolariteRepository.findById(idScolarite)` | `ResourceNotFoundException` si absent → 404 ; l'évaluation n'est alors pas chargée |
| 3 | `EvaluationRepository.findById(idEvaluation)` | `ResourceNotFoundException` si absent → 404 |
| 4 | `verifierClasse(scolarite, evaluation)` | Classe différente → `BusinessRuleViolationException`, 400 |
| 5 | `verifierBareme(request, evaluation)` | Valeur supérieure au barème → `BusinessRuleViolationException`, 400 |
| 6 | `noteMapper.toEntity(request, scolarite, evaluation)` | Construction de Note |
| 7 | `NoteRepository.save(note)` | Persistance JPA ; conflit d'intégrité ou concurrence possible |
| 8 | `noteMapper.toResponse(savedNote)` | Construction de NoteResponse |
| 9 | Retour du proxy transactionnel vers le contrôleur | Succès après validation de la transaction |

La méthode longue du premier appel est affichée **`existsBy…`**, explicitement
abrégée dans la source PlantUML ; ce n'est pas une méthode supplémentaire.

La cohérence métier compare exactement :

```java
scolarite.getClasse().getIdClasse()
evaluation.getEnseignement().getClasse().getIdClasse()
```

L'élève est celui de la scolarité choisie. Il n'existe pas d'appel à
`EleveRepository` dans ce traitement et pas de comparaison à un `idEleve` reçu.
Le doublon est défini par **scolarité–évaluation**, pas par élève–évaluation.

La valeur négative est rejetée par `@Valid` ; le service vérifie la borne haute.
Le barème n'est pas systématiquement 20. Aucun contrôle de fenêtre de saisie,
de statut de période, d'inscription active, d'année scolaire ou de date de
scolarité n'est ajouté au diagramme : `create` n'exécute pas ces règles.

La contrainte d'unicité est déclarée dans
[Note.java](../../backend/src/main/java/fr/afpa/backend/entity/Note.java) et dans
[V1__create_initial_schema.sql](../../backend/src/main/resources/db/migration/V1__create_initial_schema.sql).
Le test préalable n'est pas présenté comme une protection atomique contre deux
requêtes concurrentes : un conflit d'intégrité peut encore être levé pendant
l'écriture et converti en 409 par le gestionnaire global.

Les éventuels chargements paresseux Hibernate, le SQL généré et le commit ne sont
pas détaillés par des appels fictifs. Les quatre appels logiques aux repositories
du service sont visibles ; NoteMapper est regroupé dans les traitements du service.

## 5. Succès, retour HTTP et interface

Le contrôleur construit réellement :

```java
ResponseEntity.created(URI.create("/notes/" + response.idNote())).body(response)
```

MVC sérialise un **201 Created**, un en-tête **`Location: /notes/{idNote}`** et le
JSON de [NoteResponse.java](../../backend/src/main/java/fr/afpa/backend/dto/note/NoteResponse.java) :
`idNote`, `idScolarite`, `idEvaluation`, `valeur`, `commentaire`, `statutNote`,
`dateSaisie`. L'identifiant est généré ; la date est produite côté serveur via
`@CreationTimestamp`.

`http.js` décode le corps JSON et résout la promesse. **NotesPage n'utilise pas
l'objet retourné pour l'ajouter directement à `data.notes`**. Elle :

- affiche « Note créée. » ;
- remet `editing` à null et le formulaire à `emptyForm` ;
- incrémente `reloadKey`, déclenchant les huit GET parallèles du `useEffect` ;
- remet `busy` à false dans `finally`.

Le dernier retour utilisateur du nominal suppose que ce rechargement réussit.
Si le POST réussit mais que le rechargement échoue, la note peut déjà être créée :
le `useEffect` affiche alors son erreur de chargement. Ce cas n'est pas dessiné
comme un refus de création ni comme un rollback du POST.

## 6. Erreurs et comportement de l'interface

Source centrale :
[GlobalExceptionHandler.java](../../backend/src/main/java/fr/afpa/backend/exception/GlobalExceptionHandler.java).

| Situation | Résultat |
|---|---|
| Contrôle local invalide | Message local, aucun POST |
| JWT absent/invalide | 401, traitement de session côté frontend |
| Rôle interdit ou propriété de l'évaluation refusée | 403, pas de création dans le service |
| DTO invalide sous `@Valid` | 400 via `MethodArgumentNotValidException` ; message général « Les données envoyées sont invalides. » et `validationErrors` |
| Couple scolarité–évaluation déjà présent | 409 via `DuplicateResourceException` |
| Scolarité absente | 404, avant le chargement de l'évaluation |
| Évaluation absente dans le service | 404, avec la réserve du 403 préalable pour ENSEIGNANT |
| Classe différente ou valeur au-dessus du barème | 400 via `BusinessRuleViolationException` ; la classe est vérifiée en premier |
| Conflit d'intégrité ou d'écriture concurrente | 409 via les handlers Spring Data existants |

Les exceptions du service remontent au contrôleur puis à MVC ; ce dernier les
résout par `@RestControllerAdvice`. Les réponses applicatives du gestionnaire
comportent un `ApiErrorResponse`. Les refus produits par Spring Security ne
passent pas nécessairement par ce gestionnaire et peuvent avoir un corps vide.

Le client HTTP analyse le corps d'erreur et lève
`ApiError(message, status, responseBody)`, avec un message de repli si le serveur
ne fournit pas de `message`. `NotesPage` affiche `failure.message`, ou
« Enregistrement impossible. ». Elle ne développe pas les `validationErrors`
champ par champ et n'exécute pas les remises à zéro réservées au succès.

Pour **401**, `http.js` efface d'abord l'authentification stockée et émet
`auth:unauthorized`. [AuthContext.jsx](../../frontend/src/context/AuthContext.jsx)
met l'état d'authentification à null et
[ProtectedRoute.jsx](../../frontend/src/components/auth/ProtectedRoute.jsx)
redirige vers `/connexion`. La page peut donc être démontée avant que son message
d'erreur soit lisible. Le diagramme n'annonce pas une conservation du formulaire
après cette redirection ; il montre l'absence de reset explicite dans `catch`.

Les erreurs réseau ou un JSON malformé constituent d'autres variantes possibles,
non détaillées graphiquement. Aucun statut HTTP fictif n'est attribué à une panne
réseau et aucun mécanisme de nouvelle tentative n'est ajouté.

## 7. Fragments et choix graphiques

- Une seule source contient le nominal et les échecs, avec une numérotation
  indépendante pour chaque vue.
- Le nominal conserve les trois repositories distincts, le contrôleur et le
  service. La ligne de vie frontend regroupe les trois modules exécutés dans le
  même navigateur ; elle n'invente pas un service réseau intermédiaire.
- `opt` est réservé au contrôle de propriété pour ENSEIGNANT et au nettoyage de
  session pour un 401 : ce sont deux branches réellement conditionnelles.
- `alt` sépare les contrôles locaux des refus serveur, puis leurs phases possibles.
  La vue 2 représente des variantes d'échec : elle ne signifie pas que toutes les
  erreurs se produisent successivement dans une requête.
- Les fragments `ref` résument les huit GET réels du rechargement et renvoient à
  l'ordre des contrôles métier dans l'autre vue. Le rechargement s'étend de React
  à Spring Security/MVC : plusieurs ressources et contrôleurs sont concernés,
  sans faire passer tous les GET par NoteController. Aucun `loop` ne transforme le
  `Promise.all` en appels séquentiels.
- Les retours MVC et les exceptions remontent par leurs lignes de vie ; aucune
  flèche ne fait répondre directement NoteService au navigateur.
- Couleurs sobres : acteur vert, interface/sécurité bleu, contrôleur/service violet,
  repositories ocre, gestionnaire d'erreurs rose ; fond blanc, lignes de vie grisées.

## 8. Vérifications et limites

Les sources ont été compilées avec PlantUML 1.2025.10 déjà disponible. Plusieurs
rendus ont été examinés et ajustés : largeur des messages, regroupement du client
frontend, suppression de commentaires secondaires du corps de la figure, titres,
libellés de gardes et distinction graphique des repositories.

Le nominal n'est pas raccourci par suppression de vérifications métier : les
détails transférés à cette note concernent les signatures longues, la validation
fine du DTO et les chargements internes de la sécurité ou de Hibernate.

Contrôles réalisés : messages numérotés dans l'ordre vertical, participants et
lignes de vie identifiables, retours au bon appelant, fragments conditionnels
lisibles, absence de textes tronqués ou superposés lors de la mesure du rendu.
Les gardes restent du texte UML, sans liens hypertextes involontaires.

Les deux SVG/PNG ont été inspectés en vue complète et sur des détails. Le PDF
est contrôlé comme deux pages A3 paysage, sans image raster incorporée. Les corps
de messages correspondent à environ 8,7 points dans le nominal et 9,3 points dans
les échecs. La note de préconditions du nominal utilise désormais la même taille
que les messages (17 unités SVG, environ 8,7 points sur A3 à 100 %), sans réduire
leur police. Cet agrandissement est local à la note.
Le nominal reste dense et nécessite le format A3 ou le zoom ; aucune impression
papier n'a été réalisée. Les SVG gardent le format natif PlantUML et restent
vectoriels ; le PDF met les vues à l'échelle sans étirer leur géométrie.

Tous les diagrammes préexistants et `docs/merise/LoopingImage.jpg` sont préservés.
Le code, les permissions et les fichiers de migration n'ont pas été modifiés.
Aucune base de données, aucun Docker, test E2E, démonstration temporaire, commit
ou push n'a été exécuté ou modifié dans ce chantier.
