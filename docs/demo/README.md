# Scénario de démonstration

Utiliser uniquement des personnes et mots de passe fictifs. Démarrer PostgreSQL
avec `docker compose up -d --wait`, puis vérifier `docker compose ps`.
Sur une base vide, définir les quatre variables `BOOTSTRAP_ADMIN_*` et un
`JWT_SECRET` Base64 avant de lancer `bash mvnw spring-boot:run` dans `backend`.
Lancer `npm ci && npm run dev` dans `frontend`, puis se connecter comme ADMIN.

Dans l'interface, créer dans cet ordre :

1. un enseignant et son compte ENSEIGNANT ;
2. un responsable légal et son compte RESPONSABLE ;
3. un élève, puis sa responsabilité légale ;
4. une classe, une matière, un enseignement lié à l'enseignant ;
5. une inscription et une scolarité de l'élève dans cette classe ;
6. une période, une évaluation de l'enseignement, puis une note comprise dans le barème ;
7. consulter le bulletin et télécharger son PDF.

Se reconnecter successivement comme ENSEIGNANT et RESPONSABLE. Vérifier que
l'enseignant ne gère que ses évaluations et notes et que le responsable consulte
seulement les données de l'élève lié, sans formulaire d'écriture. La collection
Bruno permet de rejouer les appels correspondants avec l'environnement `local`.
Les JWT et mots de passe de démonstration restent hors Git.
