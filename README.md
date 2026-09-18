# Application Full Stack de gestion des élèves

Application web de gestion scolaire développée dans le cadre d’un projet de
fin de formation Concepteur Développeur d’Applications.

Le projet repose sur une API REST Spring Boot sécurisée par JWT, une interface
React et une base de données PostgreSQL.

## Fonctionnalités disponibles

### Backend

L’API permet de gérer les principales ressources du système scolaire :

- authentification et génération de JWT ;
- comptes utilisateurs et rôles ;
- élèves ;
- enseignants ;
- responsables légaux ;
- responsabilités légales ;
- classes ;
- inscriptions et scolarités ;
- matières ;
- enseignements ;
- périodes ;
- évaluations ;
- notes ;
- calcul des moyennes ;
- génération des bulletins au format PDF.

L’architecture du backend est organisée en couches :

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA / PostgreSQL
```

Les échanges avec l’API utilisent des DTO et des mappers afin de ne pas exposer
directement les entités JPA.

### Frontend

L’interface React propose actuellement :

- une page d’accueil ;
- un formulaire de connexion ;
- un espace protégé par authentification ;
- une page de consultation, recherche et administration des élèves, avec photo ;
- des écrans de gestion des classes, matières, périodes, inscriptions et scolarités pour l'administrateur ;
- des écrans d'enseignements et d'évaluations adaptés au rôle ;
- un module de consultation et de gestion des notes avec choix des évaluations et des élèves compatibles ;
- un module de calcul des bulletins ;
- le téléchargement des bulletins au format PDF ;
- une interface responsive réalisée avec Tailwind CSS.

## Rôles

L’application utilise trois rôles :

- `ADMIN` ;
- `ENSEIGNANT` ;
- `RESPONSABLE`.

L’authentification JWT et la protection des routes sont opérationnelles.

Les consultations des élèves, enseignements, évaluations, notes, scolarités et bulletins sont filtrées selon les rattachements métier de l'utilisateur. Les écrans d'administration ne sont accessibles qu'au rôle `ADMIN`. L'enseignant peut gérer les évaluations et notes de ses enseignements. Le responsable légal dispose de parcours de consultation uniquement.

## Technologies utilisées

### Backend

- Java 21 ;
- Spring Boot 3 ;
- Spring Web ;
- Spring Data JPA ;
- Hibernate ;
- Spring Security ;
- JWT ;
- Bean Validation ;
- PostgreSQL ;
- Flyway ;
- OpenPDF ;
- Maven.

### Frontend

- React 19 ;
- React Router ;
- Vite ;
- Tailwind CSS ;
- Fetch API ;
- Lucide React.

### Tests

- JUnit 5 ;
- Mockito ;
- AssertJ ;
- MockMvc ;
- Spring Security Test.

### Infrastructure et outils

- Docker Compose ;
- Git ;
- GitHub ;
- Bruno pour les requêtes API.

## Organisation du projet

```text
gestion-eleves/
├── backend/          API REST Spring Boot
├── frontend/         Interface React
├── bruno/            Collection de requêtes API
├── docs/
│   ├── merise/       MCD, MLD et MPD
│   ├── uml/          Diagrammes UML
│   └── scrum/        Backlog et rapports de sprint
└── docker-compose.yml
```

## Prérequis

Pour exécuter le projet localement :

- Java 21 ou une version compatible ;
- Node.js et npm ;
- Docker Desktop ;
- Git.

Maven n’a pas besoin d’être installé globalement, car le projet contient le
Maven Wrapper.

## Installation

Cloner le dépôt :

```bash
git clone https://github.com/Florian2524/gestion-eleves.git
cd gestion-eleves
```

### 1. Démarrer PostgreSQL

```bash
docker compose up -d
```

Vérifier l’état du conteneur :

```bash
docker compose ps
```

PostgreSQL est exposé sur le port `5432`.

Les migrations Flyway sont exécutées automatiquement lors du démarrage du
backend.

### 2. Démarrer le backend

Sous Windows PowerShell :

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Sous Linux ou macOS :

```bash
cd backend
./mvnw spring-boot:run
```

L’API est disponible sur :

```text
http://localhost:8080
```

### 3. Démarrer le frontend

Dans un second terminal :

```bash
cd frontend
npm install
npm run dev
```

L’interface est disponible sur :

```text
http://localhost:5173
```

En développement, Vite redirige les requêtes commençant par `/api` vers le
backend lancé sur le port `8080`.

## Configuration

La configuration principale du backend se trouve dans :

```text
backend/src/main/resources/application.yaml
```

Variables disponibles :

| Variable | Description | Valeur locale par défaut |
|---|---|---|
| `JWT_SECRET` | Secret utilisé pour signer les JWT | secret de développement |
| `JWT_EXPIRATION_SECONDS` | Durée de validité du JWT | `3600` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL | Valeurs de Docker Compose |
| `UPLOAD_DIRECTORY` | Dossier privé des photos | `uploads` dans le dossier de lancement |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, `BOOTSTRAP_ADMIN_NOM`, `BOOTSTRAP_ADMIN_PRENOM` | Premier administrateur | Absentes |

Les valeurs par défaut sont prévues pour le développement local. Un secret
distinct doit être utilisé dans un environnement de production.

### Première installation

Sur une base neuve, définir les quatre variables `BOOTSTRAP_ADMIN_*` avant de démarrer le backend. Choisir un mot de passe d'au moins huit caractères et un `JWT_SECRET` Base64 aléatoire. Un exemple des noms de variables figure dans `.env.example` ; ce fichier n'est pas chargé automatiquement par Spring. Au démarrage, l'application crée une personne et un compte `ADMIN` dans une transaction, avec un mot de passe hashé. Elle ignore ces variables dès qu'un compte existe. Se connecter ensuite via `/auth/login` ou l'interface. `/auth/register` exige toujours un administrateur authentifié, y compris lorsque la base est vide.

Les photos sont envoyées en multipart sur `POST /eleves/{id}/photo` (champ `file`), lues via `GET /eleves/{id}/photo` et supprimées via `DELETE /eleves/{id}/photo`. Seul `ADMIN` les modifie ; la lecture suit le droit de consultation de l'élève. Formats JPEG, PNG, GIF ou WebP, 5 Mo maximum. Le dossier `UPLOAD_DIRECTORY` doit être conservé hors du dépôt et sauvegardé avec la base.

La démarche produit et le backlog se trouvent dans [docs/scrum/README.md](docs/scrum/README.md).

## Exécuter les tests du backend

Sous Windows :

```powershell
cd backend
.\mvnw.cmd test
```

Sous Linux ou macOS :

```bash
cd backend
./mvnw test
```

Les tests couvrent notamment :

- les services métier ;
- les validations ;
- les contrôleurs REST ;
- les statuts HTTP ;
- l’authentification JWT ;
- certaines restrictions de rôle ;
- le calcul des bulletins ;
- la génération des PDF.

## État actuel

Le projet est un MVP Full Stack fonctionnel.

Les principales évolutions prévues sont :

- compléter les écrans d’administration des enseignants, responsables légaux et comptes utilisateurs dans React ;
- ajouter des tests automatisés pour l’interface React ;
- ajouter des scénarios end-to-end ;
- conteneuriser le backend et le frontend ;
- préparer une configuration de production.

## Auteur

Projet réalisé par Florian dans le cadre de la formation
Concepteur Développeur d’Applications.
