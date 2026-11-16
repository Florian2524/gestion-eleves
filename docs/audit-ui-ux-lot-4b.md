# Lot 4B — Revue responsive et accessibilité

## Méthode et limites

Revue du code des routes publiques et connectées, des composants partagés et des tests. Les contraintes de taille et points de rupture ont été examinés pour 320, 375, 768, 1024 et 1440 px. Aucun navigateur n’a été utilisé : jsdom ne mesure pas les dimensions ni les annonces d’un lecteur d’écran. Cette revue ne constitue pas une certification WCAG.

## Anomalies confirmées et corrections

- Absence de lien d’évitement : `NavigationAccessibility`, présent une seule fois dans `App`, propose « Aller au contenu principal ». Chaque page possède une cible `main#main-content`, focalisable avec `tabIndex={-1}`. Le lien apparaît au focus, au-dessus des en-têtes ; son activation focalise la cible.
- Focus inchangé après navigation React Router : le contenu principal reçoit désormais le focus quand le chemin change, y compris après redirection et retour historique. L’autofocus initial de Connexion est préservé. Une saisie, un chargement de données ou un changement de recherche/ancre ne déclenche pas ce déplacement.
- Menu mobile public sans traitement d’Échap : fermeture et retour du focus au bouton, état `aria-expanded` conservé. Aucun piège clavier ajouté. L’ancre Accueil ferme le menu et focalise le contenu ; les destinations et conditions d’authentification sont inchangées.
- Défilement JavaScript toujours animé dans Notes : `instant` lorsque la préférence de mouvement réduit est active, `smooth` sinon. Le titre du formulaire de modification reçoit le focus sans défilement supplémentaire. Aucun état métier ni payload modifié.
- Le h1 de Connexion était dans le visuel masqué sur mobile : le titre visible « Connexion » devient l’unique h1 ; le slogan conserve son apparence mais devient un paragraphe.
- Les erreurs de champ de connexion étaient associées aux contrôles mais sans annonce dynamique : ajout du rôle `alert`, en conservant les validations et `aria-describedby`.
- Les régions de tableaux défilants tabulables ne bénéficiaient pas du focus commun : ajout ciblé au sélecteur de focus.
- Les messages contenant de longues chaînes sans espaces pouvaient déborder : retour à la ligne renforcé dans `StatusMessage`. Aucun masquage horizontal global ajouté.

## Revue des largeurs dans le code — validation visuelle restante

| Zone | 320 / 375 px | 768 px | 1024 px | 1440 px |
| --- | --- | --- | --- | --- |
| En-tête public | Logo compact, menu en bloc | Menu mobile | Navigation ordinateur | Navigation ordinateur |
| En-tête connecté | Identité sécable, actions à la ligne | Présentation empilée | Présentation flexible | Largeur plafonnée |
| Connexion | Formulaire sur une colonne | Une colonne | Visuel et formulaire | Formulaire plafonné |
| Élèves / Bulletins | Cartes complètes | Tableaux dans une région défilante | Tableaux défilants si nécessaire | Conteneur plafonné |
| Notes / formulaires administratifs | Champs sur une colonne | Deux colonnes | Groupes et aides conservés | Largeur plafonnée |
| Fiches et associations | Une colonne, actions à la ligne | Plusieurs colonnes selon le module | Deux colonnes selon le module | Jusqu’à trois colonnes pour les fiches courtes |
| Tableau de bord | Une colonne | Deux colonnes | Deux colonnes pour enseignant et responsable | Jusqu’à trois colonnes pour ADMIN |

Les champs et cartes utilisent majoritairement `min-w-0`, les coordonnées longues sont sécables et les actions tactiles partagées ont une hauteur minimale de 44 px. Les largeurs minimales des tableaux restent confinées aux conteneurs défilants. Ces dispositions doivent être vérifiées dans le navigateur, notamment avec des textes longs et le zoom.

## Contrastes et structure

Vérification arithmétique de quelques paires sRGB : texte atténué sur ivoire ≈ 4,57:1 ; texte des boutons désactivés sur leur fond ≈ 8,40:1 ; bleu foncé sur jaune ≈ 9,16:1. Les textes blancs à 60 % sur les deux fonds bleus uniformes examinés dépassent 4,5:1. Ces mesures ne valident pas les superpositions sur photographie.

Les labels, rôles des messages et textes alternatifs sont conservés. Aucun calcul, accès ou traitement d’authentification n’est modifié.

## Tests

32 tests ajoutés : cible du lien d’évitement sur les 18 destinations, unicité du lien, activation clavier, focus après navigation/historique/redirection, autofocus initial, conservation de la saisie, fermeture avec Échap, absence de piège clavier et mouvement réduit. Les API sont simulées. Les tests antérieurs des autorisations restent exécutés et les assertions des alertes de champ ont été complétées.

## Vérifications manuelles restantes

1. Toutes les familles de pages aux cinq largeurs, avec noms/e-mails longs et zoom : absence de débordement de page et actions visibles.
2. Tab puis Entrée sur le lien d’évitement, navigation interne, historique et redirections : focus dans la nouvelle page, sans perturbation des formulaires.
3. Connexion : autofocus initial, unique titre principal visible, annonce des erreurs.
4. Menu mobile connecté/déconnecté : parcours complet, Échap et retour au bouton.
5. Notes avec et sans préférence de mouvement réduit : défilement et focus sur le titre de modification.
6. Lecteur d’écran : annonces des pages et messages. La quantité de contenu annoncée lors du focus du `main` peut varier selon le navigateur et le lecteur.
7. Contrastes sur photographie et visibilité du focus sur les fonds clairs et foncés, ainsi que dans les tableaux défilants.

Aucune autre anomalie certaine nécessitant une modification métier n’a été identifiée. Les dimensions et annonces réelles restent à valider. Le lot 4C n’est pas engagé.
