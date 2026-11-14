# État de reprise du sprint jury

Branche : feature/jury-finalisation, base 6eb437d.
Aucune migration réelle autorisée ; ne pas démarrer le backend sur le port SQL
de développement. Les tests créent leurs propres ressources.

## Réalisé

- Dossier jury : les neuf documents demandés et cette procédure de reprise.
- README et scénario demo séparant base neuve, base existante et tests.
- Mode --demo du lanceur E2E, démarrage/arrêt isolés vérifiés.
- Sources UML : profils MapsId, PONDERER, année nullable, séquence corrigée.
- MPD documentaire V3 distinct du MPD historique.
- Exemple Bruno période avec année scolaire.
- Mise à jour compatible du lock npm pour trois alertes modérées de l'outillage.
- Suites locales Java/React/E2E et packaging ; voir TESTS_ET_PREUVES.md.

## Dernière étape sûre et commandes

Depuis la racine, conserver LoopingImage.jpg hors de toute indexation globale.
Ne pas employer git add . ou git clean pour préparer une publication.

    git status --short
    git diff --check
    ./scripts/test-ponderer.sh
    ./scripts/test-e2e.sh

Depuis frontend : npm ci, npm run lint, npm test, npm run build,
npm audit --audit-level=high. Les commandes réseau requièrent un accès disponible.

## À terminer manuellement

1. Obtenir l'énoncé officiel et compléter la matrice si des exigences manquent.
2. Corriger Looping sur copie ; UML complet désormais rendu avec Graphviz.
3. Répéter toutes les manipulations de DEMONSTRATION.md, avec clavier/mobile.
4. Préparer captures et PDF fictifs de secours, sans données réelles.
5. Relire le dossier avec le candidat : appropriation des 35 réponses.
6. Vérifier PR et CI distante ; ne pas fusionner automatiquement.
7. Toute migration réelle V3 exige une autorisation explicite distincte.

Le sprint ne prouve pas une inspection exhaustive de chaque écran ou de chaque
ligne backend. Les résultats automatisés et les revues documentaires sont
distingués des vérifications manuelles et du déploiement de production.
