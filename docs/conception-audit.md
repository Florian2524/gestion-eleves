# Cohérence des modèles au 4 octobre 2026

Le [MPD SQL](merise/mpd-postgresql.sql) est identique, octet pour octet, à la
migration Flyway `V1__create_initial_schema.sql`. Les seize tables couvrent les
entités Java principales, dont `ResponsabiliteLegale`, `Bulletin`,
`LigneBulletin` et `CompteUtilisateur`. Les rôles du MPD sont `ADMIN`,
`ENSEIGNANT` et `RESPONSABLE`, comme dans `RoleUtilisateur`.

Les trois sources PlantUML de classes qui indiquaient au moins une
responsabilité légale par élève ont été corrigées à `0..*` : un élève peut
exister avant la création de ce lien. La séquence de saisie d'une note cite
`NoteService.create`, l'autorisation par propriété de l'évaluation et la
vérification du barème.

Les règles métier de saisie des notes ont également été alignées avec leur
sémantique HTTP. Une note supérieure au barème ou associée à une scolarité
d'une autre classe provoque désormais une erreur métier
`BusinessRuleViolationException`, exposée en `400 Bad Request`. Les erreurs
d'autorisation restent en `403 Forbidden`.

La sécurité applicative distingue les trois rôles et applique également le
périmètre métier des personnes connectées. Un enseignant ne consulte et ne
modifie que les données rattachées à ses enseignements. Un responsable légal
ne consulte que les données des élèves auxquels il est lié et ne dispose pas
d'opérations d'écriture sur les notes. La suppression, la désactivation ou le
changement de rôle du dernier administrateur actif sont refusés afin de
garantir qu'au moins un compte `ADMIN` actif reste disponible.

Ces règles sont couvertes par les tests backend ainsi que par une recette
Playwright réelle utilisant des comptes `ADMIN`, `ENSEIGNANT` et
`RESPONSABLE`. La recette vérifie notamment les données visibles et masquées,
les créations autorisées à l'enseignant, les refus hors périmètre ainsi que
l'accès du responsable au bulletin et à son PDF.

Les sept PNG correspondant aux sept sources `.puml` ont été régénérés avec
PlantUML 1.2025.10 et son moteur Smetana. Le pragma de choix du moteur a été
ajouté uniquement aux copies temporaires utilisées pour le rendu ; les sources
versionnées sont inchangées par cette génération. L'ancien export
`uml/cas-utilisation.png.png` reste un doublon historique distinct.

Les images `merise/MCD.png` et `merise/MLD.png` et le fichier source `.loo`
demandent encore une vérification visuelle dans Looping. Le fichier `.loo` est
binaire et ne fournit pas une représentation textuelle fiable des cardinalités
ou attributs.

Vérifier précisément dans le MCD que l'association
`ResponsabiliteLegale` permet **0 à plusieurs** liens du côté `Eleve`, et
qu'elle relie `Responsable` à `Eleve`. L'export MCD actuel affiche **1,n**
côté `Eleve` : cette correction visuelle reste à effectuer dans Looping.

L'export MLD affiche la table de liaison avec deux colonnes génériques
`id_personne` et `id_personne_1`. Elles doivent être renommées et vérifiées
dans Looping comme `id_responsable` et `id_eleve`, clés étrangères formant
ensemble la clé primaire conformément au MPD.

`CompteUtilisateur` affiche déjà `role` et `actif`. Comparer également les
liens de `Classe`, `Inscription`, `Scolarite`, `Enseignement`, `Evaluation`,
`Note` et `Bulletin` au MPD lors de la vérification visuelle finale.

Les bulletins et lignes de bulletin sont représentés comme entités persistées
dans le schéma. Le parcours utilisateur calcule actuellement le bulletin à la
demande ; aucun processus de validation ou d'archivage n'est modélisé.
