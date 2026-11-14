# Conformité aux exigences communiquées

Référence provisoire : liste transmise par l’utilisateur, sujet « Développement d’une application Full Stack de gestion des élèves ». Le PDF original n’est pas accessible ici et n’a pas été consulté ; comparaison manuelle encore nécessaire.

| Exigence | Fichier depuis la racine | État | Preuve / action |
|---|---|---|---|
| MCD Merise | docs/merise/MCD.png | Réalisé mais à vérifier manuellement | Guide Looping ; export historique |
| MLD | docs/merise/MLD.png | Réalisé mais à vérifier manuellement | FK et profils à revoir dans Looping |
| MPD PostgreSQL | docs/merise/mpd-courant-v3.sql | Réalisé et prouvé | Concaténation V1/V2/V3 ; migrations immuables |
| UML cas d’utilisation | docs/uml/cas-utilisation.puml | Réalisé et prouvé | Source et export ; relecture orale |
| UML classes | docs/uml/diagramme-classes-complet.puml | Réalisé et prouvé | Rendu Graphviz actuel ; vues découpées pour l’oral |
| UML séquence | docs/uml/sequence-ajouter-note.puml | Réalisé et prouvé | PNG déjà exporté ; NoteRequest et HTTP 400 |
| Java 17+ / Spring Boot 3+ | backend/pom.xml | Réalisé et prouvé | Java 21 ; Spring Boot 3 ; suite Java |
| Spring Data JPA | backend/src/main/java/fr/afpa/backend/repository/ | Réalisé et prouvé | Repositories et intégration PostgreSQL |
| Spring Security JWT | backend/src/main/java/fr/afpa/backend/config/JwtConfig.java | Réalisé et prouvé | Validation signature/expiration/compte |
| Trois rôles | backend/src/main/java/fr/afpa/backend/config/SecurityConfig.java | Réalisé et prouvé | Tests HTTP et E2E des rattachements |
| API REST | backend/src/main/java/fr/afpa/backend/controller/ | Réalisé et prouvé | DTO, validations, statuts HTTP |
| Gestion élèves | frontend/src/pages/ElevesPage.jsx | Réalisé et prouvé | CRUD et E2E |
| Gestion notes | backend/src/main/java/fr/afpa/backend/service/NoteService.java | Réalisé et prouvé | Barème, classe, droits et E2E |
| Upload photos | backend/src/main/java/fr/afpa/backend/service/ElevePhotoService.java | Réalisé et prouvé | Tests transactionnels et E2E |
| Bulletins PDF | backend/src/main/java/fr/afpa/backend/service/BulletinPdfService.java | Réalisé et prouvé | Calcul et signature PDF en tests |
| JUnit/Mockito/AssertJ/MockMvc | backend/src/test/ | Réalisé et prouvé | Suite Java complète |
| React/Tailwind | frontend/package.json | Réalisé et prouvé | Build, lint et tests |
| Responsive | frontend/src/ | Réalisé mais à vérifier manuellement | Clavier, mobile, contrastes : répétition candidat |
| PostgreSQL/Docker Compose | docker-compose.yml | Réalisé et prouvé | Compose documenté ; tests sur PostgreSQL 17 jetable |
| README/documentation | README.md, docs/jury/ | Réalisé et prouvé | Base neuve/existante séparées ; démo isolée |
| Dépôt GitHub | PR #5 | Réalisé et prouvé | Branche publiée, CI ; aucune fusion automatique |
| Suivi Scrum | docs/scrum/README.md | Réalisé mais à vérifier manuellement | Traçabilité Git prouvée ; cérémonies non attestées |

## Partie Scrum du sujet

| Exigence | État | Explication et action |
|---|---|---|
| Équipe de trois, PO formateur, Scrum Master/développeurs | Dépendant d’une validation du formateur | Aucun membre ni rôle effectivement tenu n’est attesté par les fichiers ; faire confirmer l’organisation réelle |
| Trois à quatre sprints hebdomadaires | Prévu mais non réalisé dans les preuves disponibles | Les commits datent des incréments, pas des sprints d’une semaine |
| Planning, Daily, Review, Retrospective | Prévu mais non réalisé dans les preuves disponibles | Aucune cérémonie attestée ; ne pas inventer de comptes rendus |
| Kanban To Do/In Progress/In Review/Done | Réalisé documentaire | Voir tableau de finalisation ; pas une reconstitution d’un tableau historique |
| Cartes : titre, story, responsable, branche | Réalisé documentaire | Trame fournie ; responsable à renseigner par le candidat |
| Rapports courts de sprint | Réalisé partiellement | Incréments Git factuels et trame ; pas de rapport de cérémonie inventé |
| Démonstrations 5–10 minutes | Prévu mais non réalisé dans les preuves disponibles | Répétition candidat à effectuer ; E2E ne prouve pas une review |

« Non attesté » ne permet pas de conclure qu’un événement n’a jamais eu lieu ; le candidat doit apporter les preuves réelles ou expliquer l’écart au formateur.
