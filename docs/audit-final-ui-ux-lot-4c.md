# Lot 4C — Audit final et préparation des parcours CDA

Audit local du 16 novembre 2026. Aucun serveur, navigateur graphique, backend, conteneur, migration ou test E2E n’a été lancé. Aucun accès aux bases ni changement des données de démonstration.

## Synthèse

L’audit et les tests n’ont pas identifié d’anomalie critique dans le frontend examiné. Un défaut de navigation après connexion a été reproduit et corrigé, avec un test de non-régression. Le contrôle reste limité au code et aux tests frontend simulés ; il ne remplace pas une qualification réelle des API, du PDF ou des autorisations avec les données d’un environnement isolé.

## État Git et périmètre

À l’ouverture du lot : 18 fichiers suivis modifiés, 19 fichiers non suivis, aucun fichier indexé. Le diff suivi représentait 867 insertions et 1 427 suppressions ; il ne comptabilisait pas les nouveaux composants et tests non suivis. Les suppressions de lignes concernent principalement les en-têtes dupliqués et les longs rendus JSX remplacés par les composants partagés, pas des suppressions de modules.

À la fin : 18 fichiers suivis modifiés et 21 fichiers non suivis, sans indexation. Le lot 4C modifie seulement la redirection de présentation dans `LoginPage.jsx` et ajoute `auth-journey.test.jsx` ainsi que ce rapport. Les autres fichiers préexistants sont conservés ; les empreintes ont été comparées avec l’état au début du lot.

### Fichiers suivis modifiés depuis HEAD

Tous sont sous `frontend/` :

- `index.html`, `public/favicon.svg`, `src/App.jsx`, `src/index.css`.
- `src/components/layout/AppHeader.jsx`.
- `src/components/school/PersonneChoice.jsx`, `src/components/school/ResourcePage.jsx`.
- `src/components/ui/Button.jsx`, `src/components/ui/Field.jsx`.
- `src/pages/AdministrationPages.jsx`, `BulletinsPage.jsx`, `ComptesUtilisateursPage.jsx`, `DashboardPage.jsx`, `ElevesPage.jsx`, `HomePage.jsx`, `LoginPage.jsx`, `NotesPage.jsx`, `ResponsabilitesLegalesPage.jsx`.

### Fichiers non suivis à examiner individuellement

Documentation :

- `docs/audit-ui-ux-lot-4a.md`
- `docs/audit-ui-ux-lot-4b.md`
- `docs/audit-final-ui-ux-lot-4c.md`

Composants nécessaires au fonctionnement :

- `frontend/src/components/layout/ConnectedHeader.jsx`
- `frontend/src/components/layout/NavigationAccessibility.jsx`
- `frontend/src/components/school/EvaluationPresentation.jsx`
- `frontend/src/components/school/ManagementPresentation.jsx`
- `frontend/src/components/ui/StatusMessage.jsx`
- `frontend/src/components/ui/controlStyles.js`

Tests :

- `frontend/src/test/accounts-legal-links.test.jsx`
- `frontend/src/test/auth-journey.test.jsx`
- `frontend/src/test/brand.test.jsx`
- `frontend/src/test/bulletins-evaluations.test.jsx`
- `frontend/src/test/connected-ui.test.jsx`
- `frontend/src/test/dashboard.test.jsx`
- `frontend/src/test/eleves-notes.test.jsx`
- `frontend/src/test/home-auth.test.jsx`
- `frontend/src/test/management-modules.test.jsx`
- `frontend/src/test/navigation-accessibility.test.jsx`
- `frontend/src/test/ui-accessibility.test.jsx`

Fichier personnel à préserver et à ne pas inclure par inadvertance : `docs/merise/LoopingImage.jpg`, toujours présent, 143 365 octets, empreinte inchangée pendant l’audit. Aucun fichier n’a été supprimé ou déplacé pour nettoyage.

### Vérifications de périmètre et de secrets

- Aucun diff dans `backend/`, les migrations Flyway, les configurations de base de données, les scripts, les workflows CI, les API frontend, le contexte d’authentification ou les dépendances.
- Aucun changement de routes ou de rôles autorisés. Comparaison syntaxique de l’arbre `Routes` avec HEAD : identique.
- Comparaison syntaxique de 33 fonctions sélectionnées : chargement, formatage, calcul et téléchargement des bulletins, validations et handlers d’enregistrement/suppression. Résultat : identiques à HEAD. Cette sélection n’est pas une preuve d’équivalence de toute l’application.
- Définitions des champs, références de ressources API, identifiants et validations des configurations génériques : identiques à HEAD.
- Recherche ciblée dans les textes modifiés et non suivis : aucune clé privée, clé AWS, jeton de fournisseur, JWT complet ou URL comportant des identifiants détecté. Les mots de passe et jetons littéraux des tests sont des fixtures fictives, pas des accès réels. Cette recherche n’est pas une certification exhaustive et ne couvre pas les fichiers ignorés ni tout l’historique Git.

## Problèmes classés par gravité

| Gravité | Constat | Traitement |
| --- | --- | --- |
| Moyenne, corrigée | Connexion après demande d’une route privée : la destination mémorisée était remplacée par `/espace` quand le contexte devenait authentifié. Reproduit dans le test avec le vrai `AuthProvider` et une API simulée. | Le `Navigate` de Connexion utilise la même destination mémorisée que le formulaire, avec `/espace` comme repli. Aucun changement aux contrôles d’authentification ou aux droits. |
| Faible, à prendre en compte lors d’un futur commit | Les composants partagés indispensables sont non suivis et n’apparaissent pas dans `git diff --stat`. | Inventaire explicite ci-dessus. Ne pas committer seulement les fichiers déjà suivis. Aucune indexation effectuée. |
| Faible, remarque rédactionnelle restante | L’accueil public conserve des textes génériques tels que « Saisir une note » et « Gérer les notes » même pour une session RESPONSABLE. | Pas de changement dans cet audit : la page Notes reste en consultation seule et protégée. Une adaptation rédactionnelle au rôle peut être envisagée séparément. Les descriptions du tableau de bord RESPONSABLE sont déjà adaptées. |

Aucune nouvelle refonte graphique n’a été entreprise.

## Routes et autorisations réellement implémentées

| Fonction | ADMIN | ENSEIGNANT | RESPONSABLE |
| --- | --- | --- | --- |
| Tableau de bord | 15 modules | 5 modules | 5 modules |
| Élèves et photos | Gestion | Consultation dans son périmètre | Consultation des élèves liés |
| Notes | Création, modification, suppression, consultation | Écriture autorisée ; propriété de l’évaluation/note contrôlée côté serveur | Consultation seule dans son périmètre |
| Évaluations | Gestion | Création, modification et suppression déjà autorisées ; propriété contrôlée côté serveur | Consultation seule |
| Enseignements | Gestion | Consultation | Consultation |
| Bulletins et PDF | Consultation/calcul/téléchargement | Selon scolarités accessibles | Selon scolarités accessibles |
| Classes, matières, périodes, enseignants, responsables, inscriptions, scolarités, pondérations | Routes de gestion accessibles | Routes de gestion non autorisées | Routes de gestion non autorisées |
| Comptes et responsabilités légales | Gestion | Routes non autorisées | Routes non autorisées |

Les cinq modules des deux profils non ADMIN sont : Élèves, Notes, Bulletins, Enseignements et Évaluations. Le droit d’écriture ENSEIGNANT sur Évaluations est préexistant : le parcours CDA ne doit pas le décrire comme strictement en lecture seule.

Le masquage des actions est distinct de la sécurité serveur. En lecture seule du code backend, `SecurityConfig` restreint les méthodes HTTP ; les contrôleurs utilisent également `SecurityExpressions`, des filtres de liste et des annotations d’autorisation/propriété. Les bulletins et PDF vérifient l’accès à la scolarité. Ces protections n’ont pas été modifiées ni testées sur un serveur dans ce lot.

Certains GET de ressources de référence sont accessibles aux trois rôles pour alimenter les parcours autorisés ; cela n’ouvre pas leurs pages de gestion ADMIN. Aucun accès supplémentaire n’a été ajouté.

## Non-régression et couverture

Contrôles finaux :

- `npm run lint` : réussi.
- `npm test` : **202 tests réussis dans 15 fichiers**, contre 192 avant cet audit.
- `npm run build` : réussi.
- `git diff --check` : aucune erreur d’espacement.
- `git status --short --untracked-files=all` : contrôlé, sans indexation ni opération Git destructive.

Les dix nouveaux tests utilisent le vrai `AuthProvider` et le stockage jsdom, avec les API simulées. Tout appel réseau inattendu est bloqué par un mock de `fetch`. Ils vérifient : connexion des trois rôles, retour au module demandé, déconnexion, saisie invalide, refus 401 et nouvelle tentative, connexion en cours/panne simulée, session expirée et événement de session non autorisée.

| Parcours | Preuves frontend existantes et nouvelles |
| --- | --- |
| Connexion, déconnexion, Mon espace | `auth-journey`, `home-auth`, `connected-ui`, `navigation-accessibility` |
| Modules et rôles | `dashboard`, `roles`, `accounts-legal-links`, `management-modules` |
| Recherche et gestion des élèves, photos | `eleves-notes` : création, modification, suppression, initiales/portrait, upload, retrait et formats/taille invalides |
| Personne et profil | `profils`, `management-modules`, `eleves-notes` : création, rattachement, coordonnées et conservation de la saisie |
| Notes | `roles`, `eleves-notes` : scolarités compatibles, changement d’évaluation, barème dépassé, création/modification ADMIN et ENSEIGNANT, consultation RESPONSABLE |
| Évaluations | `bulletins-evaluations` : champs, payloads, validations, erreurs et droits |
| Bulletins et PDF | `bulletins-evaluations` : sélections, arrondis affichés, valeurs absentes, calcul, nom du PDF et téléchargement simulé pour les trois rôles |
| Comptes et responsabilités légales | `accounts-legal-links`, `profils` : éligibilité, mots de passe, activation, liens et confirmations |
| Gestion générique | `management-modules`, `periodes`, `ponderations` : affichage, création/modification selon module, dates, NULL, coefficients et clé composée |
| Accessibilité et marque | `navigation-accessibility`, `ui-accessibility`, `brand`, `connected-ui` : favicon/titre, lien d’évitement, focus, menu Échap, mouvement réduit, labels et alertes |

### Limites et parcours non démontrés automatiquement

- Les tests frontend ne prouvent pas le filtrage réel des données ni le refus de requêtes malveillantes : seul le code serveur a été lu.
- Un Blob simulé vérifie le déclenchement et le nom du téléchargement PDF, pas son contenu ni la mise en page du document réel.
- jsdom ne valide ni les dimensions aux cinq largeurs, ni les annonces d’un lecteur d’écran, ni toutes les contraintes natives du navigateur, notamment `minlength`.
- Les tests ciblent des parcours représentatifs : ils ne constituent pas une couverture exhaustive de tous les CRUD, combinaisons de données et navigateurs. Aucun taux de couverture chiffré n’est revendiqué.
- Le workflow CI a été lu, pas lancé ni consulté à distance. Il définit lint/tests/build frontend, audit de dépendances, jobs backend et E2E. Aucune réussite de CI distante n’est déduite des contrôles locaux ; les jobs backend et E2E n’ont pas été exécutés ici.

## Vérifications manuelles restantes

Les lots précédents ont été validés visuellement par l’utilisateur. Cet audit n’a pas ajouté de session graphique. Vérifier spécialement la correction connexion → page demandée, puis un accès interdit après connexion, sans altérer les autorisations.

Pour une qualification complémentaire dans un environnement explicitement isolé : vérifier les données réellement visibles par chaque profil, le contenu du PDF et les erreurs d’accès ; comparer les captures aux cinq largeurs 320, 375, 768, 1024 et 1440 px ; confirmer le focus, le lien d’évitement, Échap et les messages avec un lecteur d’écran. Aucune commande de lancement, migration ou réinitialisation n’a été exécutée ou préparée pour cela.

## Liste textuelle conseillée de captures CDA

Ces propositions sont à réaliser personnellement. Aucune capture, aucun montage et aucun dossier de captures n’ont été créés. Les étapes d’écriture ci-dessous sont des scénarios proposés, pas des opérations effectuées pendant l’audit.

### Public

1. Accueil avec marque, navigation et accès Connexion, puis variante connectée « Mon espace ».
2. Connexion avec labels et erreur de saisie, sans mot de passe visible ni informations d’accès réelles.
3. Parcours d’accès direct à un module privé : connexion, puis retour au module demandé après authentification.

### ADMIN

4. Tableau de bord montrant les trois rubriques et les 15 modules.
5. Élèves : recherche, compteur, tableau ordinateur et une fiche mobile avec données fictives et portrait/initiales.
6. Formulaire élève : champs obligatoires et coordonnées ; mode de rattachement à une personne existante sur un exemple fictif autorisé.
7. Notes : évaluation sélectionnée, élèves de la classe compatible et barème explicite.
8. Tentative de note 21 sur 20 refusée, puis note valide. Le navigateur peut présenter sa validation native ; ne pas la contourner pour obtenir artificiellement un message.
9. Évaluation : fiche, enseignement, période, type et paramètres de notation ; formulaire de modification.
10. Bulletin : élève/période, résultats par matière et moyenne générale ; aperçu du PDF obtenu manuellement s’il est autorisé, après vérification de son contenu.
11. Comptes : fiches Actif/Inactif et formulaire avec mot de passe facultatif en modification, sans secret saisi ou exposé.
12. Responsabilités légales : association responsable/élève lisible et confirmation avant suppression. Annuler la confirmation suffit pour illustrer l’étape sans retirer de lien.
13. Gestion générique : classe avec année et référent, période et fenêtres de saisie, pondération avec associations non modifiables, inscription/scolarité avec dates. Choisir les exemples les plus utiles au dossier plutôt que toutes les pages.

### ENSEIGNANT

14. Tableau de bord à cinq modules et absence des rubriques administratives.
15. Consultation des élèves puis saisie/modification d’une note de son périmètre dans l’environnement autorisé.
16. Évaluations et enseignements accessibles, bulletin autorisé. Expliquer que l’écriture sur ses évaluations est déjà prévue par les permissions actuelles.

### RESPONSABLE

17. Tableau de bord à cinq modules et descriptions de consultation.
18. Élèves, notes, évaluations et enseignements accessibles sans actions d’écriture.
19. Bulletin et PDF autorisés ; exemple d’accès à une route ADMIN refusé/redirigé, sans afficher les données d’un tiers.

### Qualité et technique

20. Lien d’évitement visible au focus, menu mobile ouvert puis fermé avec Échap et présentation à 320/375 px, si utile pour démontrer l’accessibilité.
21. Résumé du terminal montrant lint, 202 tests et build réussis ; extrait d’un test sans mot de passe/jeton de fixture à l’écran.
22. Configuration CI ou résultat réel d’une exécution identifiée, si disponible : indiquer le commit et la date, sans présenter un ancien run comme validation du diff local non committé.

Utiliser uniquement des identités fictives. Vérifier aussi les noms dans les PDF et les portraits. Ne pas capturer mots de passe, jetons, stockage local, en-têtes Authorization, variables d’environnement ou données personnelles réelles.

## Recommandation pour un futur commit — aucune préparation effectuée

1. Relire le rapport et valider la correction de redirection dans le navigateur.
2. Examiner les fichiers suivis et non suivis séparément ; conserver la sélection UI/UX, ses composants nécessaires, ses tests et les documents souhaités.
3. Si un commit est ensuite autorisé, sélectionner explicitement les chemins. Éviter `git add .`, qui pourrait inclure l’image personnelle ; ne pas utiliser une option ne prenant que les fichiers suivis, qui omettrait les nouveaux composants indispensables.
4. Examiner alors le diff indexé, sa liste de fichiers et les éventuels secrets avant le commit. Refaire les contrôles si la sélection ou le code change.
5. Ne committer/pousser qu’après accord explicite. Dans ce lot : aucun `git add`, commit, push, merge, nettoyage, déplacement ou suppression.
