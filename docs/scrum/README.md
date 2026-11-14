# Démarche produit et backlog

Ce document décrit l'organisation proposée pour ce projet CDA. Il ne constitue pas un compte rendu de cérémonies tenues.

## Vision et acteurs

Centraliser les dossiers élèves, la scolarité, les évaluations, les notes et les bulletins dans une application accessible selon le rôle. L'administrateur gère les personnes et la structure scolaire. L'enseignant saisit les évaluations et notes de ses enseignements. Le responsable légal consulte les données des élèves auxquels il est lié.

## Backlog priorisé

| Priorité | Besoin | État dans le dépôt |
|---|---|---|
| P0 | Authentification JWT et droits métier par rôle et rattachement | Réalisé |
| P0 | Dossier élève : création, modification, recherche, photo sécurisée | Réalisé |
| P0 | Notes cohérentes avec classe et barème | Réalisé |
| P1 | Évaluations, scolarités et enseignements | Écrans réalisés selon les rôles |
| P1 | Classes, matières, périodes et inscriptions | Écrans d'administration réalisés |
| P1 | Calcul et export PDF des bulletins | Réalisé |
| P2 | Administration des enseignants, responsables, liens légaux et comptes dans React | Réalisé |
| P2 | Tests de parcours navigateur et déploiement complet | À faire |

## User stories

- En tant qu'administrateur, je crée un élève avec son matricule et sa date de naissance pour ouvrir son dossier.
- En tant qu'administrateur, j'ajoute ou retire sa photo sans exposer le fichier publiquement.
- En tant qu'enseignant, je saisis une note uniquement pour une évaluation de la classe concernée, dans la limite de son barème.
- En tant que responsable légal, je consulte uniquement les élèves auxquels je suis associé.
- En tant qu'utilisateur autorisé, je consulte ou télécharge le bulletin d'une scolarité à laquelle j'ai accès.

## Étapes de livraison

1. Modèle relationnel, migrations Flyway et API des ressources scolaires.
2. Authentification, rôles et contrôle du périmètre métier.
3. Parcours élèves, notes, bulletins et amorçage du premier administrateur.
4. Parcours React des classes, matières, périodes, inscriptions, scolarités, enseignements et évaluations.
5. Administration React des enseignants, responsables, liens légaux et comptes. La couverture des parcours navigateur et la préparation du déploiement restent à faire.

## Définition de terminé

Une fonctionnalité est terminée lorsque ses règles d'accès et de validation sont appliquées côté API, que son écran utilise les mêmes contrats, que les tests backend concernés passent, que `npm run lint`, `npm run build` et `git diff --check` réussissent, et que la procédure utile est documentée. Les limites restantes sont indiquées dans le backlog.

## Réconciliation du sprint jury — 14 novembre 2026

Le tableau historique ci-dessus est conservé ; ses mentions « tests navigateur
à faire » sont dépassées par la recette présente dans le dépôt et rejouée :
16 scénarios Chromium, 438 tests Java et 27 React. Le déploiement complet reste différé.
Cette mise à jour ne constitue pas la preuve d'une cérémonie Scrum.

| Priorité | Incrément / critères d'acceptation | État |
|---|---|---|
| P0 | Trois rôles, ressources autorisées et refus hors périmètre, PDF | Tests isolés réussis |
| P0 | Démonstration sans base réelle ; démarrage/arrêt jetables | Mode --demo vérifié ; répétition manuelle à faire |
| P0 | Neuf documents jury reliés aux sources | Produits, relecture candidat à faire |
| P0 | Cohérence UML/MPD avec profils, PONDERER, année | Sources actualisées ; exports manuels restants |
| P1 | Responsive, clavier et contraste | Vérification manuelle exhaustive restante |
| P1 | Publication et CI de la branche jury | Voir PR et rapport de reprise |
| P2 | Lot 3B, archivage et déploiement complet | Différés |

Chronologie factuelle issue de git log :
- 26/10 : bfe3647, PONDERER ; fusion PR #1 affcc15.
- 27/10 : 973ae98, profils multiples ; fusion PR #2 b9fc3bf.
- 27/10 : b24d47b, transaction des photos ; fusion PR #3 le 28/10, 353f9fd.
- 04/11 : be6f5d5, année scolaire ; fusion PR #4, 6eb437d.
- 14/11 : branche jury : documentation, modèles et lanceur de démonstration.

Définition de terminé complétée : résultats locaux datés, aucun secret publié,
distinction entre tests automatiques et recette manuelle, exports identifiés
comme historiques tant qu'ils ne sont pas régénérés, données réelles préservées.
Une fonctionnalité reportée ne doit pas être déclarée livrée sur la seule base
d'un document ou d'un schéma.

## Confrontation au sujet communiqué — finalisation PR #5

Le sujet envisage trois personnes, PO formateur, Scrum Master/développeurs,
trois à quatre sprints hebdomadaires, Planning/Daily/Review/Retrospective
et démos de 5–10 minutes. Les traces disponibles ne prouvent ni cette équipe,
ni ces rôles effectivement tenus, ni ces cérémonies. Le candidat doit faire
confirmer les faits par le formateur. Les dates Git ne sont pas des sprints.

Tableau documentaire actuel (pas un historique Kanban reconstitué) :

| État | Carte / user story | Responsable | Branche |
|---|---|---|---|
| To Do | Comme candidat, corriger Looping pour défendre les cardinalités | Candidat à confirmer | À créer après correction manuelle |
| In Progress | Comme candidat, répéter les trois rôles et le PDF | À renseigner par le candidat | feature/jury-finalisation |
| In Review | Comme candidat, disposer des modèles et preuves de soutenance | Auteur des commits visibles dans Git | feature/jury-finalisation, PR #5 |
| Done | Comme développeur, tester les règles sur base jetable | Auteur des commits visibles dans Git | Voir commits et CI |

Trame de rapport court à compléter après une vraie période de travail :
dates réelles, objectif, cartes terminées/non terminées, preuves Git/CI,
difficultés, actions suivantes. Une review effective peut présenter en 5–10
minutes : besoin, parcours, refus métier, PDF, tests et limites.
Ne pas remplir rétrospectivement une présence ou une validation non attestée.
