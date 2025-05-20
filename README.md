1-1.
Écrire les mots de passe directement dans le Dockerfile n’est pas une bonne idée, car ils deviennent visibles dans l’image Docker. N’importe qui qui récupère cette image pourrait donc voir ces informations sensibles.
À la place, on utilise -e pour passer les mots de passe comme variables d’environnement au moment du lancement du conteneur. Ça permet de garder ces infos secrètes et flexibles.

1-2.
Pour que les données de PostgreSQL ne disparaissent pas quand on arrête ou supprime le conteneur, on doit utiliser un volume Docker.
Ce volume fait le lien entre un dossier sur l’ordinateur hôte et le dossier des données dans le conteneur. Ainsi, les données sont conservées même si le conteneur est détruit.

1-3.
Voici comment on met en place le réseau et le conteneur PostgreSQL avec persistance des données :

On crée un réseau Docker pour connecter plusieurs conteneurs :


*docker network create app-network

On crée un dossier local pour stocker les données PostgreSQL :

*mkdir -p /my/own/datadir

On lance PostgreSQL avec montage de ce dossier local :

docker run --name=my-postgres --network=app-network -e POSTGRES_DB=db -e POSTGRES_USER=usr -e POSTGRES_PASSWORD=pwd -v C:\Users\33643\OneDrive\Documents\GitHub\devops\TP1\tp-postgres\data:/var/lib/postgresql/data postgres:17.2-alpine

On lance Adminer (interface web pour gérer la base) sur le même réseau :


docker run --name=adminer --network=app-network -p 8090:8080 -d adminer
Pour vérifier, on va sur http://localhost:8090 et on renseigne :

Serveur : my-postgres (le nom du conteneur, équivalent à l’adresse)

Utilisateur : usr

Mot de passe : pwd

Base de données : db

1-4.
La construction en plusieurs étapes (multi-stage build) est une technique où on utilise une première image lourde avec tout ce dont on a besoin pour compiler l’application (comme le JDK et Maven), puis on copie uniquement le résultat (l’application compilée) dans une image plus légère, qui ne contient que ce qui est nécessaire pour exécuter le programme (le JRE).
Ça permet d’avoir des images plus petites, plus rapides à lancer et plus sécurisées, car on ne transporte pas les outils de compilation ni le code source inutile.

1-5.
Un proxy inverse (ici Apache) reçoit toutes les requêtes web et les redirige vers le serveur qui fait le vrai travail (comme une API Spring Boot).
Cela permet de simplifier l’accès (on passe d’un localhost:8080 à un simple localhost sur le port 80), de centraliser la gestion des requêtes, et plus tard, d’ajouter des fonctionnalités comme la sécurité (SSL) ou un frontend.
Le proxy joue un rôle de "portier" qui contrôle et distribue les demandes.

1-6.
Docker Compose siimplifie la gestion d’applications qui utilisent plusieurs conteneurs.
Au lieu de lancer chaque conteneur manuellement avec plein d’options, on décrit tout dans un seul fichier YAML.
Ca facilite le démarrage, l’arrêt, et la coordination des services qui dépendent les uns des autres.

1-7.
Les commandes Docker Compose les plus importantes :

docker-compose up -d : démarre tous les services en arrière-plan (avec .env on peut ajouter --env-file .env).

docker-compose down : arrête et supprime les conteneurs, réseaux et volumes créés.

docker-compose build : reconstruit les images si on a changé le Dockerfile.

docker-compose logs : affiche les journaux de sortie des conteneurs pour voir ce qu’ils font ou détecter des erreurs.

docker-compose ps : liste les conteneurs actifs et montre leur état.


1-8 Mon fichier docker-compose.yml  :
'''
version: "3.9"

services:
  database:
  # build an image from the Dockerfile in the init-db directory
    build:
      context: ./init-db
    container_name: postgres #container name
    environment:
      POSTGRES_DB: ${POSTGRES_DB} # database name
      POSTGRES_USER: ${POSTGRES_USER} # user name
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD} # password
    volumes:
      - pgdata:/var/lib/postgresql/data # mount the volume to persist data
    networks:
      - app-network # network name
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U usr -d db"] # check if the database is ready (cmd shell to comppute, pg_isready is to check if it is ready)
      interval: 5s # check every 5 seconds
      timeout: 5s # timeout after 5 seconds 
      retries: 5 # and here retry 5 times
    



  backend:
  # build an image from the Dockerfile in the backend directory
    build:
      context: ./backend 
    environment:
      DB_HOST: ${DB_HOST}
      DB_PORT: ${DB_PORT}
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
    networks:
      - app-network # network name

    depends_on:
      - database # wait for the database to be ready
    restart: unless-stopped # restart the container unless stopped

  api:
    build:
    # build an image from the Dockerfile in the simpleapi directory
      context: ./simpleapi 
    container_name: backend-api  # container name
    environment:
      DB_HOST: ${DB_HOST}
      DB_PORT: ${DB_PORT}
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
    networks:
      - app-network # network name
      - backend-network # network name for backend
    depends_on:
      database:
        condition: service_healthy # wait for the database to be healthy
    restart: unless-stopped # restart the container unless stopped

  httpd:
    build:
      context: ./HTTP-server # build an image from the Dockerfile in the HTTP-server directory
      dockerfile: Dockerfile
    container_name: apache-proxy
    ports:
      - "${HTTP_PORT}:80" # map the host port to the container port
    networks:
      - backend-network # network name for backend
    depends_on: 
      - backend
      - api 

networks: # define the networks
  app-network:
    driver: bridge
  backend-network:
    driver: bridge
  

volumes: # define the volumes
  pgdata:
'''

https://hub.docker.com/repositories/celmds

1.9
On met nos images Docker dans un dépôt en ligne comme Docker Hub pour pouvoir les partager facilement avec l’équipe, les déployer sur d’autres machines sans tout recompiler, et garder un historique des versions, comme un GitHub pour les conteneurs
