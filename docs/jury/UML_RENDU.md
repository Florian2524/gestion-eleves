# Reproduction et contrôle des rendus UML

PlantUML 1.2025.10, mode Java headless. Graphviz 2.42.2 pour le complet
et les cas d'utilisation ; Smetana pour les autres vues. Les moteurs peuvent
modifier la disposition sans modifier le modèle.

Sur une installation disposant de Java, PlantUML et Graphviz :

    java -Djava.awt.headless=true -jar plantuml.jar -checkonly docs/uml/*.puml
    java -Djava.awt.headless=true -jar plantuml.jar -tpng docs/uml/diagramme-classes-complet.puml
    java -Djava.awt.headless=true -jar plantuml.jar -tpng docs/uml/cas-utilisation.puml

Dans cette campagne, Graphviz et ses bibliothèques ont été extraits sous /tmp,
sans installation système. GRAPHVIZ_DOT, LD_LIBRARY_PATH et GVBINDIR désignaient
cette copie. Le plugin neato absent de dépendances a émis un avertissement
pendant sa configuration ; le rendu dot utilisé a terminé sans erreur.

Le complet conserve les relations initiales : Personne est concrète, conformément
à JPA. Seuls polylignes et espacements ont été changés pour la disposition.
L'image a été examinée : classes et relations présentes, texte lisible en zoom.
Elle reste dense à projeter : préférer utilisateurs/scolarité/pédagogie à l'oral.

Les métadonnées intégrées aux PNG permettent de comparer leur source à la
version courante. L'ancien doublon cas-utilisation.png.png est historique,
distinct du PNG officiel cas-utilisation.png ; ne pas l'utiliser au jury.

La comparaison des métadonnées normalise uniquement le tiret typographique en tiret simple, transformation effectuée par PlantUML ; les relations du complet sont comparées avant/après et inchangées.
