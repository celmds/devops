# devops
1-1.
Écrire les mots de passe dans le Dockerfile les rend visibles dans l’image Docker (et donc récupérables par n'importe qui ayant accès à l'image).
Utiliser -e permet de les passer dynamiquement au moment de l’exécution.

1-2.
Nous avons besoin d’un volume attaché au conteneur PostgreSQL pour assurer la persistance des données, afin qu’elles ne soient pas perdues lorsque le conteneur est arrêté ou supprimé.
1-3.
Créer le réseau Docker :

docker network create app-network
(liaison entre les deux container)

Créer un dossier local pour stocker les données :

mkdir -p /my/own/datadir

Lancer le conteneur PostgreSQL avec montage local :

docker run --name=my-postgres --network=app-network -e POSTGRES_DB=db -e POSTGRES_USER=usr -e POSTGRES_PASSWORD=pwd -v C:\Users\33643\OneDrive\Documents\GitHub\devops\TP1\tp-postgres\data:/var/lib/postgresql/data postgres:17.2-alpine
Lancer Adminer (interface web de gestion) :


docker run \
  --name=adminer \
  --network=app-network \
  -p 8090:8080 \
  -d \
  adminer
on verifie que tout fonctionne :
Accéder à http://localhost:8090
On utilise :
Serveur : my-postgres ("l'ip" du container) donc le host
Utilisateur : usr
Mot de passe : pwd
Base de données : db

1-4 .
Une construction en plusieurs étapes nous permet de séparer la phase de construction (qui nécessite un JDK complet et des outils de construction comme Maven) de la phase d'exécution (qui n'a besoin que d'un JRE léger pour exécuter l'application).
Cela permet d'obtenir des images Docker plus petites, plus rapides et plus sûres, car l'image finale ne contient ni Maven ni JDK, mais uniquement l'application compilée.

1-5.
Un proxy inverse transmet les requêtes des utilisateurs/navigateurs web aux serveurs web. Ici, Apache en proxy inverse protège l'API Spring Boot, centralise les requêtes sur le port 80 que j'ai choisi, et permet d'ajouter SSL ou un frontend ultérieurement.
