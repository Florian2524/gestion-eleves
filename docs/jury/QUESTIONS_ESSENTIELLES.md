# Quinze questions essentielles : réponse courte et preuve

Préfixe Java ci-dessous : backend/src/main/java/fr/afpa/backend/.
Les chemins ne sont pas des preuves de réussite de test à eux seuls ; ouvrir
le fichier et montrer la règle précise. La liste complète de 35 questions reste
dans QUESTIONS_JURY.md.

| Question | Réponse orale courte | Fichier à ouvrir |
|---|---|---|
| 1. Quel problème résolvez-vous ? | Centraliser dossiers, scolarités et résultats tout en séparant administration, saisie et consultation familiale. | frontend/src/pages/DashboardPage.jsx |
| 2. Pourquoi des couches ? | HTTP dans les contrôleurs, règles et transactions dans les services, accès SQL dans les repositories. | Java service/NoteService.java |
| 3. Pourquoi des DTO ? | Maîtriser le JSON et valider les entrées sans exposer directement les entités. | Java dto/note/NoteRequest.java et mapper/NoteMapper.java |
| 4. Que garantit un JWT ? | Sa signature et son expiration, puis l'état actuel du compte sont vérifiés ; son contenu n'est pas chiffré. | Java config/JwtConfig.java, security/ActiveAccountJwtValidator.java |
| 5. Un rôle suffit-il ? | Non : un enseignant est limité à ses enseignements et un responsable à ses élèves liés. | Java security/SecurityExpressions.java |
| 6. Que fait React pour la sécurité ? | Il adapte menus et routes ; le serveur reste l'autorité. | frontend/src/components/auth/ProtectedRoute.jsx et frontend/src/App.jsx |
| 7. Pourquoi les profils multiples ? | Une personne peut être enseignant et responsable ; les profils partagent sa clé par MapsId. | Java entity/Personne.java et entity/Eleve.java |
| 8. Pourquoi PONDERER ? | Une matière a un coefficient pour une classe, même si plusieurs enseignants interviennent. | Java entity/Ponderer.java et backend/src/main/resources/db/migration/V2__create_ponderer.sql |
| 9. Comment calculer une moyenne ? | Notes normalisées sur 20, coefficients d'évaluation, puis coefficients matières ; absence de note distincte de zéro. | Java service/BulletinCalculService.java |
| 10. Comment protéger une photo ? | Autorisation sur l'élève, validation du fichier et stockage privé ; ADMIN seul modifie. | Java service/ElevePhotoService.java |
| 11. Un rollback SQL annule-t-il le disque ? | Non ; le service coordonne le nettoyage après transaction et vérifie les références validées. | Java service/ElevePhotoService.java, afterCompletion |
| 12. Comment produire le PDF ? | Calcul autorisé dans un DTO puis rendu OpenPDF ; pas d'archivage officiel revendiqué. | Java controller/BulletinController.java et service/BulletinPdfService.java |
| 13. Pourquoi Flyway ? | Versionner le schéma et empêcher sa dérive ; les migrations appliquées sont immuables. V3 réelle reste non autorisée. | backend/src/main/resources/db/migration/V3__add_annee_scolaire_periode.sql |
| 14. Quelles preuves de qualité ? | Unitaires, intégration PostgreSQL jetable, MockMvc et E2E des trois rôles ; les tests ne remplacent pas le contrôle visuel. | backend/src/test/java/fr/afpa/backend/security/AuthorizationHttpTest.java et frontend/e2e/roles-access.spec.js |
| 15. Avez-vous suivi tout Scrum ? | Je présente ce que Git/PR attestent et fais confirmer les rôles/cérémonies réels ; je n'invente ni équipe ni validation. | docs/scrum/README.md |

## Cinq questions difficiles

**JWT : un jeton signé d'un ancien ADMIN reste-t-il utilisable ?**
Pas nécessairement : ActiveAccountJwtValidator compare le compte courant,
l'état actif, l'email, la personne et le rôle du jeton. Signature correcte ne
signifie donc pas droit encore valable. Une déconnexion navigateur efface
le stockage local ; elle n'est pas une liste de révocation des jetons volés.
Le stockage local reste exposé à une XSS : ne pas promettre une sécurité production.

**Transactions : peut-on garantir zéro fichier orphelin après une panne ?**
Non. SQL et disque ne sont pas une transaction distribuée.
ElevePhotoService utilise afterCompletion, nettoyage et relecture des références
dans une nouvelle transaction. Une panne disque ou un résultat de commit inconnu
reste une limite ; le service évite les suppressions imprudentes.
Preuve : integration/ElevePhotoTransactionIntegrationTest.java sous backend/src/test.

**Flyway : une migration SQL est-elle idempotente et réversible ?**
L'exécution unique vient de l'historique Flyway, pas de IF NOT EXISTS dans V3.
Une seconde migration ne réapplique pas V3. Réécrire V1/V2 pour revenir en arrière
casserait les checksums ; aucune restauration destructive automatique n'est prévue.
V3 est additive sans backfill et doit encore être autorisée sur le développement réel.
Preuve : migration/PeriodeAnneeScolaireMigrationTest.java sous backend/src/test.

**Moyennes : deux enseignants d'une matière doublent-ils son poids ?**
Non : regroupement par matière dans BulletinCalculService, puis PONDERER une
fois par matière. Exemple : 8/10 coefficient 1 donne 16/20 ; 12/20 coefficient 2
donne (16 + 24)/3 = 13,333… ; avec une autre matière à 10 coefficient 1 et la
première coefficient matière 2 : (13,333… × 2 + 10)/3 = 12,22 à l'affichage.
Les moyennes brutes, pas les valeurs affichées déjà arrondies, sont pondérées.

**Cardinalités : pourquoi zéro responsable pour un élève est-il permis ?**
Le dossier peut être créé avant le lien légal. Chaque ligne de responsabilité
référence exactement un responsable et un élève, et le couple est unique ;
cela n'impose pas au moins une ligne par élève. Cette règle se distingue de la
cardinalité profil/personne (un profil a une personne, qui peut avoir zéro profil).
Preuves : migration V1, entity/ResponsabiliteLegale.java et GUIDE_LOOPING.md.
