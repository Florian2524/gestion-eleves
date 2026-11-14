# Démonstration

Utiliser uniquement l'environnement jetable :

    ./scripts/test-e2e.sh --demo

Voir [la procédure complète](../jury/DEMONSTRATION.md) : personnes fictives,
ordre des manipulations, résultats attendus et secours hors ligne.

Ne pas démarrer Compose ou Spring Boot sur la base locale existante :
le backend appliquerait V3, non autorisée.
La recette automatique indépendante se lance avec ./scripts/test-e2e.sh.
