# Tests et preuves — campagne du 14 novembre 2026

Référence initiale : 6eb437d12d6493ba2143ccd37b8b1b36205b7de0.
Environnement local : Java 21.0.12.1, Node 22.22.0, PostgreSQL 17 jetable.
Les résultats ci-dessous sont locaux ; ils ne sont pas présentés comme une CI distante.

| Commande depuis la racine | Résultat vérifié |
|---|---|
| ./scripts/test-ponderer.sh | 438 tests, 0 échec, 0 erreur, 0 ignoré |
| Maven package via scripts/test-e2e.sh | BUILD SUCCESS |
| cd frontend : npm run lint | Réussi |
| cd frontend : npm test | 27 tests, 4 fichiers réussis |
| cd frontend : npm run build | Réussi |
| ./scripts/test-e2e.sh | 16 scénarios Chromium réussis |
| ./scripts/test-e2e.sh --demo | Backend, frontend et PostgreSQL V3 démarrés sur ports dynamiques ; arrêt Ctrl-C testé |
| bash -n scripts/test-e2e.sh | Réussi |
| git diff --check | Réussi avant publication |

Les tests Java comprennent MockMvc, sécurité/JWT, profils multiples,
PONDERER, notes/barèmes, calcul/PDF, transactions photo et migrations V1/V2/V3.
Les noms de preuves : AuthorizationHttpTest, ActiveAccountJwtValidatorTest,
PeriodeAnneeScolaireMigrationTest, PondererMigrationTest,
PersonneProfilsIntegrationTest, ElevePhotoTransactionIntegrationTest.
Ils résident dans backend/src/test/java/fr/afpa/backend/.

Playwright couvre CRUD administration, photos, profils cumulables, périodes
avec année/NULL et contrôles ENSEIGNANT/RESPONSABLE avec PDF et accès refusés.
Les fixtures sont synthétiques ; le compte applicatif n'est pas superutilisateur.
Aucun test n'utilise le conteneur de développement.

Journaux locaux de cette campagne : /tmp/ge-jury-evidence/.
Rapports régénérables : backend/target/surefire-reports et
frontend/playwright-report. Ne pas publier JWT, mots de passe, données privées.
La première tentative E2E a été interrompue pendant l'édition du script :
seule la relance complète réussie constitue la preuve.

À vérifier manuellement : démonstration complète en direct, apparence des PDF,
responsive multi-écran, contrastes et navigation clavier exhaustive.
Le lancement --demo seul ne prouve pas les 18 manipulations manuelles.
Historique CI : https://github.com/Florian2524/gestion-eleves/actions
Les résultats distants de la nouvelle branche doivent être consultés séparément.

L'installation npm ci --offline a réussi avec le cache présent. Son message
« 0 vulnérabilité » hors ligne n'est pas une preuve d'audit actualisé :
l'audit en ligne a trouvé trois alertes modérées ESLint/config-array/minimatch.
Le correctif compatible et ses contrôles sont consignés dans le bilan du sprint.
GitHub main a été vérifiée via git ls-remote avec accès réseau autorisé.

Après npm audit fix sans --force : zéro vulnérabilité signalée par le registre.
Le package.json reste inchangé ; le lock met notamment ESLint/config-array et
minimatch à jour dans les plages compatibles. Lint, 27 tests React et build
ont été relancés avec succès sur cet arbre de dépendances.
