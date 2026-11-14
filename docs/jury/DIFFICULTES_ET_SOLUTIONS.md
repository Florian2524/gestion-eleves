# Difficultés réelles et solutions traçables

| Difficulté | Solution et preuve |
|---|---|
| Coefficient répété par enseignement | PONDERER, clé classe/matière ; commit bfe3647 ; tests PondererMigrationTest et BulletinCalculServiceTest |
| Héritage exclusif incompatible avec profils cumulables | Composition MapsId ; commit 973ae98 ; PersonneProfilsIntegrationTest |
| Fichier photo supprimé malgré rollback SQL | Synchronisation après transaction et relecture des références ; commit b24d47b ; docs/PHOTOS_TRANSACTIONS.md |
| Année scolaire facultative et anciens clients | Distinguer JSON absent de NULL explicite ; commit be6f5d5 ; PeriodeAnneeScolaireIntegrationTest |
| Éviter les migrations accidentelles | Scripts de tests créant PostgreSQL 17 en tmpfs, port aléatoire, rôle séparé ; runner réel hors Git non utilisé |
| Documents en retard sur le code | Sources UML réconciliées ; exports historiques signalés ; MPD V1 préservé et MPD courant distinct |
| DNS indisponible | Poursuite locale, preuves et texte PR ; aucune publication inventée |

Les PR #1, #2, #3 et #4 apparaissent dans les commits de fusion locaux.
Ces éléments ne prouvent ni réunions Scrum ni échanges client.
Pour l'oral, expliquer une erreur, sa conséquence, la solution et le test qui
empêche sa réapparition ; ne pas annoncer avoir personnellement vécu un
événement dont seule la trace de code est disponible.
