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

## Fiche courte à garder à côté de Looping

Les libellés exacts de menus peuvent dépendre de la version : vérifier la cible
avant validation plutôt que suivre un menu supposé identique.

1. Enregistrer une copie du .loo sous gestion-eleves-jury.loo ; conserver les
   originaux et le JPEG personnel sans modification.
2. Dans le MCD, sélectionner la patte ELEVE de RESPONSABILITE_LEGALE :
   minimum 0, maximum n. Ne pas changer automatiquement l'autre patte.
3. Dans le MLD de la copie, suivre chaque FK de la table de liaison :
   vers RESPONSABLE → id_responsable ; vers ELEVE → id_eleve. Si la structure
   générée est ambiguë, arrêter le renommage et faire confirmer la cible.
4. Contrôler la PK : ensemble des deux FK, pas deux clés primaires indépendantes ;
   aucune clé artificielle supplémentaire. Le couple ne doit pas se répéter.
5. Vérifier PERSONNE : coordonnées communes, PK partagée des trois profils,
   cumul autorisé. Ne pas modéliser une spécialisation exclusive.
6. Vérifier PONDERER : CLASSE/MATIERE, PK du couple et coefficient_matiere.
   Conserver ENSEIGNEMENT et son miroir SQL ; ne pas confondre coefficient d'évaluation.
7. Ajouter/vérifier PERIODE.annee_scolaire : facultative au MCD ; VARCHAR(9)
   nullable au MPD, CHECK années consécutives. Ne déduire aucune année des dates.
8. Exporter MCD-jury.png et MLD-jury.png, ouvrir à 100 %, relire les deux
   extrémités, PK/FK et profils ; comparer au MPD courant et au guide.
   Faire valider les conventions par le formateur avant remplacement des PNG.

Contrôle visuel minimum : un élève sans lien légal reste représentable ; un
responsable peut être lié à plusieurs élèves ; une personne peut cumuler les
profils ; un couple classe/matière n'a qu'un coefficient ; année NULL possible.
Ces manipulations sont à réaliser par le candidat, pas déjà exécutées ici.
