# Démonstration reproductible et isolée

## Préparation

Java 21, Node 22.22+, Docker et les dépendances locales sont nécessaires.
Depuis la racine : ./scripts/test-e2e.sh --demo.
Le lanceur construit le JAR, crée PostgreSQL 17 neuf en tmpfs, un rôle applicatif
non superutilisateur, des ports loopback dynamiques et des uploads temporaires.
Il affiche l'URL et le compte ADMIN fictif. Garder le terminal ouvert.
Ne jamais lancer Compose ni un backend sans cible SQL explicitement isolée.

Les identifiants ADMIN sont éphémères et affichés seulement dans ce terminal.
Pour les comptes créés à la main, choisir des mots de passe de démonstration
sans réutiliser ceux de développement. Exemples de personnes :
Alice Martin (enseignante), Claire Demo (responsable), Jules Demo (élève),
et un second élève non lié. Domaines example.test exclusivement.

## Manipulations et résultats attendus

1. Ouvrir l'URL affichée ; connexion ADMIN ; présenter les modules de l'espace.
2. Créer Alice, numéro employé DEMO-ENS-01, puis son compte ENSEIGNANT.
3. Créer Claire puis son compte RESPONSABLE.
4. Créer Jules, matricule DEMO-ELEVE-01, date de naissance fictive.
5. Importer un petit PNG synthétique ; recharger et vérifier la photo.
6. Créer le lien Claire–Jules dans les responsabilités légales.
7. Créer une classe DEMO-CDA, année 2026-2027, et une matière DEMO-MATH.
8. Créer l'enseignement Alice–classe–matière ; vérifier sa pondération.
9. Créer l'inscription de Jules, puis sa scolarité dans la classe.
10. Créer une période : septembre à décembre 2026, année 2026-2027.
11. Créer une évaluation sur 20, coefficient 1, dans cette période.
12. Déconnexion ; connexion Alice ; voir uniquement son enseignement.
13. Saisir 15/20 pour Jules ; tenter 21/20 et montrer le refus.
14. Consulter le bulletin ; pour une unique note admissible, attendre 15/20.
15. Télécharger le PDF ; vérifier nom fictif, période, matière et moyenne.
16. Déconnexion ; connexion Claire ; consulter Jules, ses notes et son PDF.
17. Montrer l'absence des formulaires de modification et le refus d'une route
    ADMIN. Avec la recette automatisée, montrer aussi un accès à un autre élève
    refusé côté API : le simple masquage d'un menu ne prouve pas la sécurité.
18. Revenir ADMIN ; présenter l'année facultative et l'absence de backfill.

Les scénarios automatisés indépendants se lancent par ./scripts/test-e2e.sh.
Ils reconstruisent leurs fixtures ; ils ne conservent pas un dossier de démo.

## Secours et remise à zéro

Avant le jury, répéter hors Internet après téléchargement des dépendances,
de Chromium et de l'image postgres:17. Docker, Java et Node restent requis.
Conserver localement un PDF fictif et des captures de la répétition ; les
présenter comme captures, pas comme un système en direct.

Ctrl-C dans le terminal du lanceur arrête ses processus et supprime uniquement
son conteneur et ses uploads. Relancer --demo fournit une base neuve.
Ne jamais utiliser down -v ni supprimer un volume de développement.
La recette complète manuelle de ces 18 étapes doit encore être répétée par
le candidat ; les tests E2E constituent des preuves complémentaires.
