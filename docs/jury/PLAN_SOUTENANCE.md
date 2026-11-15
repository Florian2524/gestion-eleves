# Plan oral adaptable

Aucune durée officielle n'a été fournie. Adapter la place de la démonstration
au temps accordé ; conserver une version courte de chaque partie.

1. Présentation personnelle : expliquer son rôle réel, sans inventer de client.
2. Contexte : une école doit centraliser dossiers, scolarités, notes et bulletins.
   Transition : « Ces besoins conduisent à trois usages distincts. »
3. Acteurs : ADMIN, ENSEIGNANT, RESPONSABLE ; montrer un exemple de cloisonnement.
4. Organisation : backlog priorisé, incréments visibles dans Git, limites connues.
   Ne pas annoncer des cérémonies Scrum sans trace.
5. Merise : partir d'une personne, puis profils multiples ; expliquer PONDERER.
6. UML : suivre une saisie de note, de React à PostgreSQL.
   Transition : « Le modèle devient maintenant une application en couches. »
7. Architecture : API REST, DTO, services, JPA, contraintes et migrations.
8. Sécurité : JWT valide ET compte actif ET accès à la ressource.
9. Démonstration : utiliser DEMONSTRATION.md et exclusivement la base jetable.
10. Qualité : montrer les résultats réellement obtenus, un refus 403 et un test
    de migration ; distinguer couverture automatisée et inspection manuelle.
11. Difficultés : profils multiples, coefficients et transaction des photos.
12. Limites : année scolaire sans règles temporelles avancées, archivage différé.
13. Conclusion personnelle : ce qui a été appris et ce qui serait priorisé ensuite.

Préparer les sources à ouvrir : SecurityConfig, NoteService, Ponderer,
ElevePhotoService, un test de migration. Montrer une règle et sa preuve plutôt
que parcourir tous les fichiers. Prévoir une capture des résultats locaux et
un PDF fictif produit pendant la répétition, sans données réelles.

## Trame principale à dire avec ses propres mots

« Ce projet de formation répond à un besoin de gestion scolaire : les mêmes
données doivent être utilisables par l'administration, les enseignants et les
responsables, avec des droits différents. » Montrer les cas d'utilisation.

« J'ai séparé la personne et ses profils, car une même personne peut être à la
fois enseignant et responsable. » Montrer le MCD après correction manuelle,
puis diagramme-classes-utilisateurs.png et Eleve.java (@MapsId).
Ne pas présenter le PNG Merise actuel comme déjà corrigé.

« Une requête traverse React, le contrôle JWT, le contrôleur, le service et la
base. » Montrer sequence-ajouter-note.png, NoteController puis NoteService.
Distinguer rôle, propriété de l'évaluation et contrôle du barème.

« Pour les résultats, chaque note est normalisée sur 20, puis pondérée ; le
coefficient matière dépend de la classe. » Montrer Ponderer.java et le calcul.
Passer à la démonstration avec FICHE_DEMONSTRATION.md.

« La fiabilité repose sur plusieurs niveaux de tests, avec des bases jetables. »
Montrer la CI main du merge 0f833e9 et les résultats datés, pas une capture sans
référence : 438 Java, 27 React et 16 Playwright dans la campagne documentée.
Les chiffres sont des résultats observés, pas une promesse de couverture totale.

« Les difficultés les plus importantes ont été les profils cumulables,
PONDERER et la coordination des photos avec les transactions. »
Choisir un seul exemple, expliquer risque, solution et test de régression.

« Le MVP n'archive pas encore des bulletins officiels ; la cohérence temporelle
avancée et le durcissement de production sont différés. » Terminer par les
apprentissages personnels réels ; ne pas inventer client ou cérémonie Scrum.

## Version courte de secours

1. Besoin et trois rôles : un dossier scolaire, trois périmètres d'accès.
2. Un schéma : profils multiples et coefficient classe/matière.
3. Un parcours : note autorisée → refus hors barème → bulletin/PDF.
4. Une preuve : CI et test de sécurité avec accès hors périmètre.
5. Une difficulté et une limite : transaction photo ; archivage différé.

Si le direct est indisponible, annoncer les captures et le PDF comme preuves
enregistrées. Ne pas simuler une application en fonctionnement.

## Onglets à préparer, dans cet ordre

- docs/uml/cas-utilisation.png : acteurs et fonctionnalités.
- docs/uml/diagramme-classes-utilisateurs.png : profils/personne.
- docs/uml/diagramme-classes-pedagogie-bulletins.png : notes et PONDERER.
- docs/uml/sequence-ajouter-note.png : trajet de la requête.
- backend/src/main/java/fr/afpa/backend/controller/NoteController.java.
- backend/src/main/java/fr/afpa/backend/service/NoteService.java.
- backend/src/main/java/fr/afpa/backend/security/ActiveAccountJwtValidator.java.
- backend/src/main/java/fr/afpa/backend/service/ElevePhotoService.java.
- frontend/src/components/auth/ProtectedRoute.jsx : protection UI distincte de l'API.
- docs/jury/TESTS_ET_PREUVES.md et la CI main correspondant au commit montré.

Le diagramme complet Graphviz sert de référence en zoom ; les vues découpées
sont plus adaptées à une projection. Aucune durée officielle n'est supposée.
