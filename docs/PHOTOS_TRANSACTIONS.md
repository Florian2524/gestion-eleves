# Cohérence transactionnelle des photos d’élèves

Correctif ciblé sur `fix/photo-rollback-transactionnel`, depuis `b9fc3bf8392b43f5ce49c01f764dbfff70d9ed8e`. Aucun changement de tables, migration, DTO, rôle ou route HTTP.

## Cause

`ElevePhotoService` écrivait le nouveau fichier, modifiait `photo_url`, puis supprimait immédiatement l’ancien fichier. Le commit SQL effectué par le proxy Spring arrive après le retour de la méthode. Un rollback ou un échec tardif rétablissait donc la référence SQL vers un fichier déjà supprimé, et pouvait abandonner le nouveau fichier. La suppression d’une photo avait le même décalage.

## Comportement retenu

Les mutations exigent une véritable transaction Spring d’écriture avec synchronisation active. Elles prennent le verrou de personne commun aux mutations de profils, puis chargent l’élève. Cela sérialise les remplacements concurrents et le retrait du profil, sans changer les règles de suppression du profil lui-même.

Pour un enregistrement/remplacement :

1. Valider le type, la taille et la signature selon les règles existantes.
2. Enregistrer une `TransactionSynchronization` avant l’effet de bord disque.
3. Créer exclusivement un fichier à nom UUID généré par le serveur ; ne jamais écraser un fichier existant. Marquer sa création avant l’écriture, pour permettre sa compensation si celle-ci échoue partiellement.
4. Écrire le contenu et modifier `photo_url` dans la transaction existante.
5. Dans `afterCompletion(STATUS_COMMITTED)`, nettoyer les fichiers candidats devenus inutiles, après vérification de leurs références SQL validées.
6. Dans `afterCompletion(STATUS_ROLLED_BACK)`, nettoyer uniquement le nouveau fichier effectivement créé, s’il n’est pas référencé. Ne pas supprimer l’ancien.

Pour un retrait, seule la référence SQL est retirée pendant la transaction ; l’ancien fichier n’est candidat au nettoyage qu’après le commit. En cas de rollback, sa référence et son contenu restent disponibles.

Une transaction peut faire plusieurs remplacements/retraits successifs. Le nettoyage examine les références finales, pas uniquement l’ordre des appels. Même le fichier nouvellement créé est vérifié après commit : il peut avoir été remplacé à nouveau ou ne plus être la référence finale.

La relecture des références utilise `REQUIRES_NEW` et une transaction en lecture seule. Elle évite de réutiliser la transaction terminée ou son contexte JPA, qui peuvent encore être liés au thread lors du callback. Un fichier encore référencé par un autre élève est conservé. Les noms existants ne sont pas réaffectables par les DTO de création/modification d’élève ; les mutations applicatives génèrent toujours un nouveau nom UUID. Les écritures SQL externes contournant ce protocole ne sont pas synchronisées avec le système de fichiers.

## Erreurs et confinement

| Situation | Comportement |
|---|---|
| Fichier invalide ou élève absent | Erreur habituelle 400/404 ; pas de fichier créé |
| Pas de transaction Spring d’écriture | Refus avant tout effet de bord disque |
| Échec de création/écriture | Exception ; rollback SQL ; compensation du fichier créé si nécessaire ; ancien fichier conservé |
| Échec SQL ou veto avant commit | Rollback réel ; ancien fichier conservé et nouveau fichier nettoyé |
| Échec de nettoyage après commit | SQL reste validé ; aucune exception de nettoyage propagée au client ; fichier conservé et événement ERROR `PHOTO_CLEANUP_FAILED`, avec résultat transactionnel et nom UUID |
| Échec de nettoyage après rollback | Référence SQL précédente conservée ; événement ERROR `PHOTO_CLEANUP_FAILED`, fichier à traiter conservé |
| Référence SQL encore présente | Pas de suppression du fichier |
| Référence SQL impossible à vérifier | Pas de suppression ; erreur de nettoyage explicite |
| Résultat transactionnel inconnu | Conservation prudente des fichiers ; événement ERROR `PHOTO_CLEANUP_UNKNOWN` |

Les erreurs de nettoyage doivent être surveillées dans les journaux et traitées : vérifier les références SQL et l’état du stockage avant toute reprise manuelle. Aucune suppression en masse ni recherche automatique d’orphelins dans le dossier réel n’est introduite.

Le client ne reçoit pas une erreur de transaction SQL pour une ancienne photo que le stockage refuse d’effacer après un commit réussi. Le remplacement reste HTTP 200 et le retrait HTTP 204 ; les erreurs de nettoyage restent observables et ne sont pas ignorées silencieusement.

Les noms fournis par le navigateur ne servent jamais de chemin de stockage. Les chemins persistés doivent respecter le motif UUID/extensions existant. Les lectures et écritures n’empruntent pas de lien symbolique ; une suppression d’un lien ne supprime pas sa cible. Les fichiers extérieurs au répertoire configuré ne sont pas touchés.

## Limites explicites

PostgreSQL et le disque ne participent pas à une transaction distribuée. Ce mécanisme compense les rollbacks dans un processus vivant ; il ne garantit pas l’exécution des callbacks après un arrêt brutal de la JVM, une coupure électrique ou un résultat de commit indéterminé. Il ne promet pas non plus une durabilité commune des écritures disque et PostgreSQL lors d’une panne machine. De tels incidents peuvent nécessiter une réconciliation ; aucun outbox, travailleur de reprise ou migration n’est ajouté dans ce correctif.

Le nettoyage peut échouer si le disque ou la base ne sont pas disponibles. Il conserve alors les fichiers et produit les erreurs ci-dessus, sans suppression risquée ni fausse erreur SQL. Il n’existe pas de relance automatique : l’exploitation doit surveiller et reprendre ces erreurs.

La transaction indépendante de vérification requiert une connexion supplémentaire du pool pendant le callback. Une saturation du pool peut donc différer le nettoyage et produire une erreur explicite ; elle ne doit pas provoquer la suppression d’un fichier sans vérification. Les erreurs d’accès et l’insuffisance de capacité restent des cas opérationnels à surveiller.

Une lecture concurrente ayant déjà vu une ancienne référence peut croiser un remplacement validé et devoir relire la ressource ; ce mécanisme n’ajoute pas de rétention des anciennes versions pour les lecteurs historiques. Les auteurs SQL ou disque externes, un répertoire de stockage contrôlé par un autre écrivain, les savepoints manuels et la modification directe d’un contexte JPA hors du protocole applicatif ne constituent pas une transaction distribuée prise en charge.

La suppression d’un profil élève conserve son comportement existant du lot 2 : aucune suppression automatique de sa personne, de ses autres profils ou de leurs données. Aucun nouveau nettoyage de photos lors du retrait du profil n’est ajouté.

## Tests et validation

`ElevePhotoTransactionIntegrationTest` utilise PostgreSQL 17 isolé et des dossiers temporaires d’images synthétiques. La classe de test n’a pas de transaction englobante annulée automatiquement : `TransactionTemplate`, les méthodes Spring proxifiées et MockMvc franchissent de vraies frontières de commit et rollback. Les tests relisent après `flush`/`clear` et dans de nouvelles transactions.

Couverture : premier enregistrement et rollback, remplacement réussi et rollback reproduisant l’audit, veto tardif avant commit, erreur SQL après flush, retrait réussi et rollback, fichiers invalides, échec d’écriture, échecs de nettoyage après commit/rollback, HTTP 200/204 malgré ces erreurs post-commit, élève absent, fichiers partagés encore référencés, référence conservée par une transaction indépendante, plusieurs mutations par transaction, référence finale modifiée, traversée et liens symboliques, refus en transaction de lecture et concurrence.

La concurrence est vérifiée sur deux connexions physiques distinctes : avant de libérer la première transaction, le test observe via `pg_blocking_pids` que la seconde attend effectivement son verrou PostgreSQL. Les deux transactions sont ensuite attendues jusqu’à leur terminaison, avant le nettoyage des fixtures.

Le test `auditRegressionReplacementRollbackRestoresReferenceAndFiles` a d’abord été exécuté contre l’ancien service dans une copie privée : échec attendu, l’ancien fichier ayant déjà disparu. Il passe avec le correctif. Les fichiers du dossier réel `uploads` et la base PostgreSQL de développement ne sont pas utilisés par ces tests.

Les suites complètes Java, React et Playwright sont exécutées sur copie de la branche avec des comptes SQL applicatifs non superutilisateurs, des bases neuves et un stockage PostgreSQL temporaire distinct de tout volume de développement. Les résultats ci-dessous décrivent la validation précédant la publication ; ces essais n’utilisent pas la base ou les photos réelles.

Validation du 27 octobre 2026 : **421 tests Java réussis**, dont **21 nouveaux tests d’intégration photo** (et les 3 tests unitaires photo adaptés), aucun échec/erreur/test ignoré ; compilation et packaging Maven réussis. **16 tests React et 15 scénarios Playwright Chromium réussis**, ainsi que le lint et le build frontend. Les suites profils multiples, sécurité, PONDERER, notes et bulletins restent vertes. Les tests HTTP confirment explicitement les réponses 200/204 malgré les erreurs de nettoyage post-commit, avec journalisation des erreurs.

Référence Spring : [`TransactionSynchronization.afterCompletion`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/support/TransactionSynchronization.html#afterCompletion(int)), notamment la nécessité d’une transaction distincte pour les accès transactionnels effectués depuis ce callback.
