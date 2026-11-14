# Matrice de conformité

Référence : documents du dépôt et mission jury. Aucun énoncé PDF trouvé dans
le dépôt ; conformité à un sujet officiel non certifiée. Chemins depuis la racine.

| Exigence | Implémentation / fichier | Preuve | Statut | Action |
|---|---|---|---|---|
| MCD | docs/merise/MCD.png | conception-audit.md | Partiellement conforme | Corriger patte ELEVE dans Looping |
| MLD | docs/merise/MLD.png | audit modèles | Partiellement conforme | Identifier et renommer FK responsabilité |
| MPD | docs/merise/mpd-courant-v3.sql | migrations officielles | Conforme documentaire | Ne pas exécuter sur base existante |
| Cas d'utilisation | docs/uml/cas-utilisation.puml | source | À vérifier manuellement | Comparer au scénario oral |
| Classes UML | docs/uml/diagramme-classes-*.puml | MapsId/PONDERER/année | Partiellement conforme | Régénérer PNG |
| Séquence | docs/uml/sequence-ajouter-note.puml | NoteRequest, HTTP 400 | Partiellement conforme | Régénérer PNG |
| JWT/comptes | JwtConfig, ActiveAccountJwtValidator | tests sécurité Java | Conforme automatisé | Durcissement production distinct |
| Rôles/ressources | SecurityConfig, SecurityExpressions | AuthorizationHttpTest, roles-access.spec.js | Conforme automatisé | Répétition orale |
| CRUD élèves | EleveController/Service, React | tests Java/E2E | Conforme automatisé | Répéter manuellement |
| Notes/barèmes | NoteService | tests métier/HTTP | Conforme automatisé | Montrer refus 21/20 |
| Photos | ElevePhotoService | ElevePhotoTransactionIntegrationTest | Conforme automatisé | Photo fictive |
| Bulletins/PDF | BulletinCalculService, BulletinPdfService | tests calcul/PDF/E2E | Conforme automatisé | Ouvrir PDF en lecteur |
| Couches/erreurs/DTO | controller/service/repository/exception/dto | suite Java | Conforme automatisé | Expliquer un parcours |
| Connexion/déconnexion | AuthContext, authStorage | E2E rôles | Conforme automatisé | Session expirée manuelle |
| Formulaires/navigation | frontend/src/pages | React/E2E | Partiellement conforme | Inspection visuelle |
| Responsive/contrastes | frontend/src/index.css, composants | pas de mesure exhaustive | À vérifier manuellement | Téléphone/clavier |
| PostgreSQL/Flyway | scripts/test-ponderer.sh | tests migration | Conforme automatisé | Base réelle non migrée |
| Docker | scripts tmpfs | compte test non superutilisateur | Conforme tests | Déploiement complet différé |
| README | README.md | base neuve/existante séparées | Partiellement conforme | Répéter hors ligne |
| Démonstration | scripts/test-e2e.sh --demo | procédure dédiée | À vérifier manuellement | Répétition complète candidat |
| Tests | TESTS_ET_PREUVES.md | campagne locale | Voir preuves | Distinguer CI et local |
| GitHub | feature/jury-finalisation | commits/PR si réseau | À vérifier | Publication/CI |
| Scrum | docs/scrum/README.md | chronologie Git | Conforme documentaire | Pas de cérémonie inventée |
| Dossier jury | docs/jury/ | neuf documents | Conforme documentaire | Relecture candidat |

Points d'attention : exports obsolètes, répétition de démo, absence d'énoncé officiel.
La migration réelle V3 n'est pas un prérequis à la démonstration isolée.
