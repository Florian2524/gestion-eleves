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

## Réconciliation du sprint jury — 14 novembre 2026

Les sources PlantUML décrivent désormais les profils associés par MapsId,
PONDERER et Periode.anneeScolaire facultative. La séquence utilise NoteRequest
et HTTP 400 pour les violations du barème/classe. Six PNG ont ensuite été régénérés avec PlantUML 1.2025.10/Smetana. Le diagramme complet reste historique : le moteur a échoué et son ancien export a été préservé.
Le MPD V1 est conservé ; mpd-courant-v3.sql rassemble les migrations pour lecture.

Looping, à effectuer sur une copie du .loo : dans RESPONSABILITE_LEGALE,
mettre la patte ELEVE à 0,n ; dans le MLD identifier chaque FK par sa table
cible, puis nommer id_responsable et id_eleve. Vérifier la PK composée et
exporter sous de nouveaux noms. Ne jamais modifier LoopingImage.jpg.

## Clôture des rendus et exigences — finalisation PR #5

Le diagramme complet a finalement été rendu avec Graphviz 2.42.2 et PlantUML
1.2025.10, après correction de Personne concrète et ajustements de disposition
(polylignes, espacement). Aucune association n'a été supprimée. Le PNG des cas
d'utilisation est également régénéré avec Graphviz. Les autres exports restent
ceux des sources courantes, vérifiés par leurs métadonnées.
Le complet est un document de référence à zoomer ; les vues découpées sont
préférables sur vidéoprojecteur. Les mentions précédentes d'échec restent
l'historique des essais, pas l'état du livrable final.
Le guide GUIDE_LOOPING.md décrit les seules corrections Merise encore manuelles.
