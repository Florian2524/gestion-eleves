# Correction Looping sur copie — à réaliser par le candidat

Aucune modification du .loo ou des PNG Merise n'est revendiquée ici.
Looping n'est pas disponible dans cet environnement. Préserver l'original
gestion-eleves.loo et LoopingImage.jpg ; travailler sur une copie nommée
gestion-eleves-jury.loo et exporter sous des noms nouveaux.

| Élément | Manipulation à vérifier dans Looping | Justification / résultat |
|---|---|---|
| RESPONSABILITE_LEGALE côté ELEVE | Ouvrir la patte, mettre minimum 0 maximum n | Un élève peut exister avant toute responsabilité |
| FK de la liaison | Identifier d'abord la table cible de chaque FK générée ; nommer id_responsable et id_eleve | Ne pas déduire la cible du seul suffixe _1 |
| PK liaison | Sélectionner les deux FK comme clé primaire composée | Unicité du couple, conforme au MPD |
| PERSONNE/profils | Vérifier une PK/FK partagée pour ELEVE, ENSEIGNANT et RESPONSABLE ; spécialisation non exclusive | Une personne peut cumuler plusieurs profils ; chaque profil a exactement une personne |
| PONDERER | Association CLASSE/MATIERE, coefficient_matiere, PK composée | Coefficient unique par couple ; ne pas le confondre avec coefficient_evaluation |
| ENSEIGNEMENT | Conserver les liens enseignant/classe/matière | Son coefficient SQL miroir reste soumis à PONDERER, FK et triggers V2 |
| PERIODE | Ajouter annee_scolaire facultative de longueur 9 dans le modèle physique | V3 autorise NULL, format AAAA-AAAA et années consécutives |
| Scolarité/notes | Vérifier FK élève/classe et FK note/scolarité/évaluation | Comparer chaque relation au MPD courant, sans inventer de règle temporelle |

Dans le MCD, représenter les identifiants conceptuels ; ne pas ajouter
arbitrairement les FK générées du MLD comme nouveaux attributs métier.
Comparer INSCRIPTION, SCOLARITE, ENSEIGNEMENT, EVALUATION, NOTE, BULLETIN,
LIGNE_BULLETIN et COMPTE_UTILISATEUR aux sources SQL et JPA.

Vérification finale : ouvrir les nouveaux exports à 100 %, lire les deux
extrémités de chaque association, contrôler PK/FK et possibilité de cumuler
les profils. Faire confirmer les conventions Merise au formateur.
Ne remplacer les exports versionnés qu'après cette validation visuelle.
Capturer avant/après et conserver la copie source. Aucun SQL ne doit être
exécuté sur PostgreSQL réel pour cette correction documentaire.
