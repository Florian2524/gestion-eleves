# Répétition personnelle : cases à cocher et preuves

Cette checklist est à exécuter par le candidat ; elle n'atteste aucune
répétition manuelle passée. Utiliser scripts/test-e2e.sh --demo exclusivement.

- [ ] Vérifier l'URL dynamique et la base neuve ; garder le terminal ouvert.
- [ ] Connexion ADMIN : capturer le tableau de bord sans exposer le mot de passe.
- [ ] Créer enseignant, responsable et leurs comptes fictifs.
- [ ] Créer élève et photo PNG synthétique ; capturer la photo après rechargement.
- [ ] Relier le responsable à l'élève ; créer un autre élève non lié.
- [ ] Classe, matière, enseignement et pondération cohérente.
- [ ] Inscription puis scolarité ; période 2026-2027 ; évaluation sur 20.
- [ ] Connexion ENSEIGNANT : saisir 15/20, refuser 21/20 ; capturer le message.
- [ ] Calculer le bulletin ; télécharger et ouvrir le PDF fictif.
- [ ] Connexion RESPONSABLE : voir son élève et PDF, pas l'autre.
- [ ] Montrer un refus API hors périmètre ; masquer un bouton ne suffit pas.
- [ ] Vérifier clavier, focus, petit écran, chargements et états vides.
- [ ] Déconnexion, rafraîchissement et reconnexion.
- [ ] Ctrl-C : processus et conteneur de démo supprimés, fichiers réels intacts.

À conserver : captures ADMIN/ENSEIGNANT/RESPONSABLE, refus, PDF fictif,
résultats locaux et lien CI. Aucun JWT, secret ou donnée réelle sur les captures.

Panne Internet : dépendances, Chromium et image postgres:17 doivent déjà
être téléchargés ; sinon montrer les captures comme une démonstration enregistrée.
Panne applicative : arrêter la démo, expliquer le symptôme, utiliser le PDF
fictif de secours et les preuves CI, sans toucher à la base de développement.

## Les huit points à savoir expliquer personnellement

1. Le besoin scolaire et les trois acteurs.
2. PERSONNE et profils MapsId cumulables, distincts du rôle du compte.
3. JWT signé, expiration, compte actif et contrôle de ressource.
4. Notes normalisées sur 20 et les deux niveaux de coefficients.
5. Transactions SQL et limites du nettoyage des fichiers photo.
6. Flyway V1/V2/V3 ; V3 réelle non autorisée, historique préservé.
7. Différence unitaire/intégration/E2E et portée réelle des preuves.
8. Écarts Scrum honnêtes, limites temporelles et archivage différé.

## Ordre de préparation avant le jury

1. Comparer CHECKLIST_LIVRABLES.md au PDF original ; confirmer les numéros US.
2. Exécuter les huit étapes courtes de GUIDE_LOOPING.md sur une copie ; valider les exports.
3. Préparer les comptes et données fictifs puis suivre FICHE_DEMONSTRATION.md,
   en notant les identifiants ; répéter avec déconnexion entre les rôles.
4. Tester clavier/mobile, ouvrir le PDF et enregistrer les captures de secours.
5. Répéter PLAN_SOUTENANCE.md, puis sa version courte ; répondre sans lire aux
   quinze questions de QUESTIONS_ESSENTIELLES.md.
6. Faire confirmer les faits Scrum par le formateur. Ne pas compléter une
   cérémonie ou un rôle non attesté avec des faits inventés.

Facultatif après le jury : cohérence temporelle avancée, archivage officiel,
durcissement production. Ces développements ne sont pas nécessaires à cette répétition.
