# Cohérence des modèles au 19 septembre 2026

Le [MPD SQL](merise/mpd-postgresql.sql) est identique, octet pour octet, à la
migration Flyway `V1__create_initial_schema.sql`. Les seize tables couvrent les
entités Java principales, dont `ResponsabiliteLegale`, `Bulletin`,
`LigneBulletin` et `CompteUtilisateur`. Les rôles du MPD sont `ADMIN`,
`ENSEIGNANT` et `RESPONSABLE`, comme dans `RoleUtilisateur`.

Les trois sources PlantUML de classes qui indiquaient au moins une
responsabilité légale par élève ont été corrigées à `0..*` : un élève peut
exister avant la création de ce lien. La séquence de saisie d'une note cite
désormais `NoteService.create`, l'autorisation par propriété de l'évaluation
et la vérification du barème. Le code renvoie actuellement `403` pour un
barème dépassé ou une classe incompatible.

Les sept PNG correspondant aux sept sources `.puml` ont été régénérés avec
PlantUML 1.2025.10 et son moteur Smetana. Le pragma de choix du moteur a été
ajouté uniquement aux copies temporaires utilisées pour le rendu ; les sources
versionnées sont inchangées par cette génération. L'ancien export
`uml/cas-utilisation.png.png` reste un doublon historique distinct.

Les images `merise/MCD.png` et `merise/MLD.png` et le fichier source `.loo`
demandent une vérification visuelle dans Looping. Le fichier `.loo` est binaire
et ne fournit pas une représentation textuelle fiable des cardinalités ou
attributs. Vérifier précisément dans le MCD que l'association
`ResponsabiliteLegale` permet **0 à plusieurs** liens du côté `Eleve`, et
qu'elle relie `Responsable` à `Eleve`. L'export MCD actuel affiche **1,n**
côté `Eleve` : c'est une correction visuelle requise dans Looping. L'export
MLD affiche la table de liaison avec deux colonnes génériques `id_personne`
et `id_personne_1` ; les renommer et les vérifier dans Looping comme
`id_responsable` et `id_eleve`, clés étrangères formant ensemble la clé
primaire selon le MPD. `CompteUtilisateur` affiche déjà `role` et `actif`.
Comparer aussi les liens de `Classe`, `Inscription`,
`Scolarite`, `Enseignement`, `Evaluation`, `Note` et `Bulletin` au MPD. Leur
source graphique n'a pas été modifiée automatiquement.

Les bulletins et lignes de bulletin sont représentés comme entités persistées
dans le schéma. Le parcours utilisateur calcule le bulletin à la demande ;
aucun processus de validation ou d'archivage n'est modélisé.
