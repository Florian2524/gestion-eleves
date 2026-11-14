# Merise et UML : arguments et vérifications

Le MCD exprime les concepts métier et cardinalités ; le MLD les relations,
PK et FK ; le MPD leur réalisation PostgreSQL. UML complète ces modèles avec
acteurs, classes applicatives et séquences d'appels.

| Modèle | Fichier | Statut |
|---|---|---|
| MCD | docs/merise/MCD.png | Export à corriger manuellement dans Looping |
| MLD | docs/merise/MLD.png | Noms des deux FK de responsabilité à confirmer |
| Source Looping | docs/merise/gestion-eleves.loo | Binaire préservé |
| MPD historique | docs/merise/mpd-postgresql.sql | V1 conservée |
| MPD courant | docs/merise/mpd-courant-v3.sql | V1/V2/V3 documentaires |
| Cas d'utilisation | docs/uml/cas-utilisation.puml | Source disponible |
| Classes | docs/uml/diagramme-classes-*.puml | Sources réconciliées ; six PNG régénérés, complet historique |
| Séquence note | docs/uml/sequence-ajouter-note.puml | DTO et HTTP corrigés |

PERSONNE porte les coordonnées communes. ELEVE, ENSEIGNANT et RESPONSABLE
sont des profils à PK/FK partagée : une personne peut cumuler plusieurs profils.
Le code utilise OneToOne/MapsId, pas un héritage Java des entités.
Chaque profil référence une personne ; une personne a zéro ou un profil de
chaque type. Un compte porte un rôle unique : profils multiples ne signifient
pas plusieurs rôles simultanés dans un JWT.

RESPONSABILITE_LEGALE relie un responsable et un élève par une clé composée.
Un élève peut être créé avant tout lien : cardinalité 0,n. Pour corriger
Looping, ouvrir une copie du .loo, changer cette patte, identifier les FK du
MLD par leurs cibles, les nommer id_responsable/id_eleve, vérifier la PK
composée puis exporter sous un nouveau nom. Ne pas retoucher le JPEG personnel.

PONDERER associe classe et matière avec un coefficient unique pour ce couple.
ENSEIGNEMENT conserve un coefficient miroir SQL, maintenu par FK et triggers V2 ; PONDERER est la référence du calcul.
ENSEIGNEMENT associe enseignant, classe et matière ; plusieurs enseignants
peuvent contribuer à la même moyenne matière sans doubler son coefficient.
INSCRIPTION et SCOLARITE représentent des informations distinctes ; le MVP
ne garantit pas encore une couverture temporelle exhaustive entre elles.

V1 pose le schéma ; V2 déplace les coefficients dans PONDERER ; V3 ajoute
PERIODE.annee_scolaire nullable, VARCHAR(9), avec CHECK de deux années
consécutives. Le lot profils change le mapping JPA sans nouvelle migration.
Les périodes historiques restent NULL. Le format n'impose pas la cohérence
des dates et de l'année scolaire : décision différée au lot 3B.

Les entités BULLETIN/LIGNE_BULLETIN existent dans le schéma, mais le parcours
courant génère un calcul et un PDF à la demande. Ne pas prétendre disposer
d'un archivage validé. Six PNG sont régénérés avec PlantUML 1.2025.10/Smetana. Le diagramme complet seul reste historique : erreur interne du moteur, ancien PNG préservé. Utiliser les vues découpées actualisées pour l’oral.
