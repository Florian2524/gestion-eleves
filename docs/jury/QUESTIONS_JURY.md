# Questions du jury et réponses fidèles au projet

1. **Pourquoi Spring Boot ?** Il intègre HTTP, injection, sécurité et JPA. Les règles scolaires restent dans les services.
2. **Pourquoi React ?** Les composants et routes structurent les écrans ; les contrôles API restent indispensables.
3. **Pourquoi PostgreSQL ?** Les relations, PK, FK, CHECK et transactions correspondent au domaine scolaire.
4. **Pourquoi Merise ?** Pour définir concepts et cardinalités avant les tables.
5. **Pourquoi UML ?** Pour expliquer acteurs, classes et déroulement des requêtes.
6. **MCD, MLD, MPD ?** Concepts métier ; relations et clés ; réalisation SQL avec types et contraintes.
7. **Pourquoi Flyway ?** Pour tracer les versions du schéma et leurs checksums. Les migrations appliquées restent immuables.
8. **Pourquoi des DTO ?** Pour stabiliser le contrat JSON, valider les entrées et éviter d'exposer le graphe JPA.
9. **Pourquoi des couches ?** Contrôleur : HTTP ; service : métier et transaction ; repository : accès aux données.
10. **Comment fonctionne JWT ?** Un jeton signé HS256 est émis à la connexion ; signature, expiration et compte courant sont vérifiés. Il n'est pas chiffré.
11. **Comment Spring Security autorise ?** SecurityConfig filtre les routes, les contrôles métier vérifient ensuite les ressources et rattachements.
12. **Compte désactivé ?** ActiveAccountJwtValidator relit le compte : un ancien jeton peut être refusé malgré sa signature valide.
13. **Profils multiples ?** PERSONNE est indépendante ; ELEVE, ENSEIGNANT et RESPONSABLE utilisent OneToOne/MapsId. Ce n'est plus un héritage Java.
14. **Plusieurs profils, plusieurs rôles ?** Non. Le compte et son JWT ont un rôle unique, distinct des profils.
15. **Pourquoi PONDERER ?** Le coefficient dépend du couple classe/matière, pas de l'enseignant.
16. **Comment calculer la moyenne ?** Note × 20 / barème, pondération des évaluations, puis des matières par PONDERER. Les matières sans note ne contribuent pas au dénominateur général.
17. **Pourquoi BigDecimal ?** Pour expliciter les calculs décimaux ; calcul à dix décimales, affichage à deux avec HALF_UP.
18. **Photos protégées ?** Lecture selon droit sur l'élève, écriture ADMIN, taille et format contrôlés, répertoire privé.
19. **Comment générer le PDF ?** BulletinCalculService produit un DTO, BulletinPdfService le rend avec OpenPDF après contrôle d'accès.
20. **Transaction SQL et fichiers ?** Ils ne forment pas une transaction distribuée ; ElevePhotoService utilise des notifications de fin de transaction.
21. **Rollback photo ?** Le nouveau fichier non référencé peut être nettoyé ; l'ancien fichier valide doit rester disponible. Des tests d'intégration le vérifient.
22. **Unitaire ou intégration ?** Unitaire : règle isolée, souvent mocks ; intégration : couches et vrai PostgreSQL jetable.
23. **Comment tester Flyway ?** Migrer un état antérieur avec fixtures, comparer données et séquences, vérifier l'historique et la non-réapplication.
24. **Pourquoi Playwright ?** Pour exercer navigateur, React et API ensemble avec les trois rôles.
25. **Accès à un autre élève ?** Filtrage des listes et contrôle de l'identifiant demandé ; tests hors périmètre avec refus.
26. **400, 401, 403, 404 ?** Données invalides ; authentification invalide ; droit insuffisant ; ressource absente. Un doublon métier peut produire 409.
27. **Mot de passe stocké ?** Un hash via l'encodeur de sécurité, jamais le mot de passe en clair.
28. **Premier ADMIN ?** FirstAdminBootstrap utilise les quatre variables BOOTSTRAP_ADMIN sur base neuve ; inscription publique non ouverte.
29. **Année nullable ?** Pour préserver l'historique sans inventer d'année. V3 ajoute VARCHAR(9) et CHECK sans backfill.
30. **Format année suffisant ?** Non : deux années consécutives ne garantissent pas la cohérence avec les dates/classes, différée au lot 3B.
31. **Bulletins archivés ?** Le schéma contient les entités, mais le parcours actuel calcule à la demande sans workflow officiel d'archivage.
32. **Pourquoi une démo isolée ?** La base locale contient des données et V3 réelle reste non autorisée.
33. **Quelle difficulté présenter ?** Profils multiples, coefficients et photos : relier chaque choix à son commit et son test, sans inventer d'incident.
34. **Quelles évolutions ?** Durcissement production, cohérence temporelle et archivage après validation des besoins.
35. **Prêt pour production ?** Non démontré : HTTPS, secrets, déploiement, supervision et stratégie de session exigent une préparation distincte.

Pour approfondir chaque réponse, ouvrir ARCHITECTURE.md et les sources citées.
