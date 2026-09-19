# Interface React de gestion des élèves

L'interface utilise React 19, React Router, Vite et Tailwind CSS. Elle comprend la connexion, les dossiers élèves, les notes, les bulletins et les modules d'administration scolaire. Les droits sont contrôlés par les routes React et par l'API Spring Boot.

Node.js 22.22.0 ou plus récent est requis par `react-router@8.3.0`. Le fichier `.nvmrc` indique la version de référence.

```bash
nvm use
npm ci
npm run dev
```

Vite redirige `/api` vers le backend local sur le port 8080. Les vérifications disponibles sont `npm run lint` et `npm run build`. Voir le [README principal](../README.md) pour l'installation complète.
