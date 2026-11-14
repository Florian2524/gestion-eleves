# Architecture expliquée pour la soutenance

Le navigateur exécute React ; Vite sert le développement et transmet /api au
backend. La production doit prévoir HTTPS et un reverse proxy : le serveur
Vite n'est pas une solution de déploiement.

| Couche | Responsabilité | Source depuis la racine |
|---|---|---|
| Interface | Routes et affichage selon rôle | frontend/src/App.jsx |
| Session | Stockage et connexion/déconnexion | frontend/src/context/AuthContext.jsx |
| Transport | Appels REST et gestion des erreurs | frontend/src/api/ |
| Filtrage HTTP | Routes, rôles, refus par défaut | backend/src/main/java/fr/afpa/backend/config/SecurityConfig.java |
| JWT | Signature HS256 et validation temporelle | backend/src/main/java/fr/afpa/backend/config/JwtConfig.java |
| Compte courant | Désactivation et jeton obsolète | backend/src/main/java/fr/afpa/backend/security/ActiveAccountJwtValidator.java |
| Ressources | Contrats HTTP, DTO validés | backend/src/main/java/fr/afpa/backend/controller/ |
| Métier | Transactions et règles de cohérence | backend/src/main/java/fr/afpa/backend/service/ |
| Accès SQL | Repositories Spring Data | backend/src/main/java/fr/afpa/backend/repository/ |
| Modèle | Entités et associations | backend/src/main/java/fr/afpa/backend/entity/ |
| Schéma | Migrations immuables | backend/src/main/resources/db/migration/ |

Un DTO sépare contrat HTTP et modèle persistant. Les mappers traduisent les deux.
JPA ne remplace pas les contraintes SQL ; les deux niveaux protègent des erreurs
différentes. Le contrôleur protège l'accès, le service les règles métier.

Les rôles ne suffisent pas : SecurityExpressions contrôle les rattachements
aux enseignements et aux élèves. RESPONSABLE consulte seulement les élèves liés ;
ENSEIGNANT écrit dans son périmètre ; ADMIN administre. Le masquage des boutons
React ne constitue pas une autorisation.

Le JWT est signé, pas chiffré. JwtConfig exige une clé Base64 d'au moins 256 bits.
ActiveAccountJwtValidator relit le compte, son rôle, sa personne et son état actif.
Le stockage navigateur expose le jeton en cas de XSS : ne pas présenter ce MVP
comme une configuration de production.

ElevePhotoService coordonne SQL et fichiers avec les notifications de fin de
transaction. Un rollback retire le nouveau fichier si aucune référence validée
ne le conserve ; un commit permet de nettoyer l'ancien. Une panne de nettoyage
reste possible : voir docs/PHOTOS_TRANSACTIONS.md.

BulletinCalculService normalise les notes sur 20 et regroupe par matière.
Les coefficients d'évaluation pondèrent les notes, PONDERER les matières pour
la classe. Les matières sans note ne contribuent pas au dénominateur général.
Les BigDecimal évitent les erreurs binaires ; affichage à deux décimales.
BulletinPdfService produit le PDF avec OpenPDF. Le parcours courant calcule à
la demande et n'archive pas un bulletin officiel.
