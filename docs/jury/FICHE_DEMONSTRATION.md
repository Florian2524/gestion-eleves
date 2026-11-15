# Fiche de répétition : écran, geste, résultat, explication

Cette fiche est un support candidat, pas l'attestation d'une répétition personnelle.
La préparation suit [DEMONSTRATION.md](DEMONSTRATION.md).
Démarrer depuis la racine : ./scripts/test-e2e.sh --demo ; utiliser uniquement
l'URL et le compte ADMIN fictif affichés. Garder le terminal ouvert.
Vérifier les ports dynamiques ; ne jamais démarrer le backend sur la base locale.

## Jeu fictif minimal

Alice Martin, enseignante DEMO-ENS-01 ; Claire Demo, responsable ;
Jules Demo, élève DEMO-ELEVE-01 ; un second élève non lié à Claire.
Emails example.test, mots de passe distincts des comptes réels.
Classe DEMO-CDA 2026-2027 ; matière DEMO-MATH ; coefficient matière 1 ;
enseignement Alice/classe/matière commençant le 01/09/2026 ; inscription et
scolarité de Jules couvrant la période ; période septembre–décembre 2026,
année 2026-2027, statut OUVERTE ; évaluation sur 20, coefficient 1.
Utiliser uniquement les valeurs acceptées proposées par les formulaires.

| Écran / route | Geste candidat | Résultat attendu | Phrase à expliquer | Revenir en cas d'erreur |
|---|---|---|---|---|
| Connexion /connexion | ADMIN fictif du terminal | Espace /espace | Le compte est amorcé uniquement sur base neuve | Vérifier URL et saisie ; pas de compte réel |
| Enseignants /enseignants puis comptes /comptes-utilisateurs | Créer Alice puis compte ENSEIGNANT lié à Alice | Profil et compte distincts | Personne, profil et rôle ont des responsabilités différentes | Corriger le profil existant plutôt que le dupliquer |
| Responsables /responsables puis comptes | Créer Claire et compte RESPONSABLE | Compte associé au bon profil | Le rôle ne donne pas accès à tous les élèves | Vérifier personne sélectionnée |
| Élèves /eleves | Créer Jules, ajouter PNG synthétique | Fiche/photo après rechargement | ADMIN écrit ; la lecture photo suit le droit sur l'élève | Corriger le formulaire ; réimporter un petit PNG valide |
| Liens /responsabilites-legales | Relier Claire à Jules ; garder un autre élève non lié | Couple enregistré | La responsabilité détermine le périmètre de lecture | Vérifier les deux identités, pas seulement les noms |
| Classes /classes, matières /matieres | Créer DEMO-CDA et DEMO-MATH | Référentiels disponibles | Classe porte l'année ; matière porte son identité | Réutiliser l'existant si doublon |
| Enseignements /enseignements | Associer Alice/classe/matière, coefficient 1 | Enseignement visible | PONDERER fixe le coefficient commun classe/matière | Vérifier /ponderations si coefficient incompatible |
| Inscriptions /inscriptions puis /scolarites | Inscrire Jules puis scolarité dans DEMO-CDA | Scolarité sélectionnable pour la note | La note porte sur une scolarité, pas seulement une personne | Vérifier élève/classe/dates |
| Périodes /periodes | Créer période avec année 2026-2027 | Année affichée | Champ facultatif ; aucune année historique inventée | Corriger format ; ne pas modifier une période réelle |
| Évaluations /evaluations | Évaluation liée à l'enseignement et période, barème 20 | Évaluation disponible | Deux niveaux de coefficients : évaluation et matière | Vérifier les parents avant de recréer |
| Déconnexion puis /connexion | Se connecter comme Alice | Évaluations de son périmètre | API et interface contrôlent des choses différentes | Revenir ADMIN pour corriger compte/rattachement |
| Notes /notes | Essayer 21/20, constater le refus, puis saisir 15/20 | Erreur puis note valide | Le serveur vérifie barème et classe | Garder le formulaire ; ne pas créer deux notes du même couple |
| Bulletins /bulletins | Choisir scolarité et période, calculer | Avec une seule note admissible, moyenne 15/20 | Normalisation et pondération, pas une moyenne de moyennes arrondies | Vérifier période, classe, notes et coefficients |
| Bulletin PDF | Télécharger puis ouvrir | Document fictif cohérent | Même droit de consultation, rendu OpenPDF | Fermer le lecteur ; recalculer et retélécharger |
| Déconnexion puis connexion Claire | /eleves, /notes, /bulletins | Jules visible, autre élève absent ; PDF autorisé | Le rattachement limite les données | Revenir ADMIN vérifier le lien légal |
| Route ADMIN /comptes-utilisateurs comme Claire | Ouvrir la route | Redirection de protection frontend | Ce refus UI seul ne prouve pas le refus API | Revenir /espace |
| Refus API, seulement sur démo | Depuis les outils réseau ou un client local, GET /api/comptes-utilisateurs avec JWT Claire | HTTP 403 ; sans JWT, 401 | Même un appel direct est contrôlé | Vérifier le rôle ; ne jamais copier le JWT dans une capture |

## Pièges à éviter

Essayer 21 puis 15 avant de créer la note évite qu'un doublon masque le refus
de barème. Si une note existe déjà, la modifier plutôt que la recréer.
Ne pas deviner l'identifiant d'un autre élève : noter les identifiants fictifs
pendant la préparation. Ne jamais montrer un token ou un mot de passe à l'écran.
Le backend expose /notes ; Vite transmet /api/notes vers lui.

## Captures et secours

Conserver : tableau de bord ADMIN, photo fictive, note refusée, bulletin,
PDF, vue RESPONSABLE et réponse 403 masquant les en-têtes d'authentification.
Hors Internet : image postgres:17, dépendances et Chromium doivent être déjà
présents. Si l'application tombe, montrer captures/PDF en les annonçant comme
preuves enregistrées ; ne pas dépanner en pointant sur la base réelle.
Ctrl-C détruit uniquement l'environnement du lanceur ; relancer fournit une
base vierge. Prévoir une répétition complète avant le jour du jury.

## Contrôle technique du 15 novembre 2026

Sur le lanceur --demo : instance PostgreSQL en tmpfs, aucun volume monté,
historique V1/V2/V3 réussi, un compte ADMIN actif, frontend HTTP 200 et API
sans jeton HTTP 401. Ports dynamiques distincts ; arrêt Ctrl-C effectué.
Aucun test ni backend lancé sur PostgreSQL réel.
Le candidat doit encore exécuter personnellement les manipulations du tableau.
Aucun fichier technique n'a été modifié dans cette préparation documentaire ;
les résultats 438/27/16 précédents restent des preuves historiques datées,
pas une nouvelle campagne complète présentée artificiellement comme exécutée.
