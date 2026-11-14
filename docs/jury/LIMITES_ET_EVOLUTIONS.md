# Limites et évolutions

## Avant présentation

- Énoncé PDF non trouvé dans le dépôt : exigences évaluées à partir des documents
  disponibles et de la mission, pas d'un sujet officiel consulté.
- Répéter manuellement toute la démonstration, le responsive et les contrastes.
- Corriger les exports Looping et régénérer le seul UML complet resté historique ; six autres PNG ont été régénérés.
- Contrôler les dépendances en ligne et la CI distante quand le réseau fonctionne.
- PostgreSQL réel reste V2 : ce sprint n'autorise ni sa migration ni son backend.

## Limites du MVP

- Pas de workflow de validation/archivage officiel des bulletins.
- Lot 3B différé : cohérence année/dates, chevauchements, clôture, mobilité,
  fenêtres de saisie et dérogations.
- SQL et fichiers ne forment pas une transaction distribuée. Nettoyage différé
  des fichiers et traitement de panne restent nécessaires.
- Jeton dans le navigateur : risque XSS ; prévoir HTTPS, politique CSP et stratégie
  de session adaptée avant production.
- Secrets locaux de développement à remplacer en production.
- Durcir le rôle SQL applicatif réel dans une intervention distincte ; les tests
  utilisent déjà un rôle non superutilisateur.
- Pas de déploiement complet conteneurisé frontend/backend revendiqué.
- Les verrous Flyway ne bloquent pas tous les écrivains SQL indépendants.
