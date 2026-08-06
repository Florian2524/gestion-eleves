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
- une page de consultation et de recherche des élèves ;
- un module de consultation et de gestion des notes ;
- un module de calcul des bulletins ;
- le téléchargement des bulletins au format PDF ;
- une interface responsive réalisée avec Tailwind CSS.

## Rôles

L’application utilise trois rôles :

- `ADMIN` ;
- `ENSEIGNANT` ;
- `RESPONSABLE`.

L’authentification JWT et la protection des routes sont opérationnelles.

La restriction fine des données selon le périmètre métier reste à compléter.
Par exemple, un responsable légal devra à terme accéder uniquement aux données
des élèves auxquels il est associé.

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

Les valeurs par défaut sont prévues pour le développement local. Un secret
distinct doit être utilisé dans un environnement de production.

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

- filtrer les données selon le rôle et le périmètre de l’utilisateur ;
- compléter les écrans d’administration dans React ;
- ajouter le CRUD des élèves dans le frontend ;
- ajouter la gestion des évaluations dans le frontend ;
- ajouter des tests automatisés pour l’interface React ;
- ajouter des scénarios end-to-end ;
- conteneuriser le backend et le frontend ;
- préparer une configuration de production.

## Auteur

Projet réalisé par Florian dans le cadre de la formation
Concepteur Développeur d’Applications.
