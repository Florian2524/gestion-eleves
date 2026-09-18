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
| P2 | Administration des enseignants, responsables et comptes dans React | À faire |
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
5. À venir : administration React des autres personnes, couverture des parcours navigateur et préparation du déploiement.

## Définition de terminé

Une fonctionnalité est terminée lorsque ses règles d'accès et de validation sont appliquées côté API, que son écran utilise les mêmes contrats, que les tests backend concernés passent, que `npm run lint`, `npm run build` et `git diff --check` réussissent, et que la procédure utile est documentée. Les limites restantes sont indiquées dans le backlog.
