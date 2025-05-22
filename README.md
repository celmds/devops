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
```

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
```


https://hub.docker.com/repositories/celmds

1.9
On met nos images Docker dans un dépôt en ligne comme Docker Hub pour pouvoir les partager facilement avec l’équipe, les déployer sur d’autres machines sans tout recompiler, et garder un historique des versions, comme un GitHub pour les conteneurs

2-1
Les Testcontainers sont une librairie de test (dispo en Java, Python, Node.js, etc.) qui permet de lancer des conteneurs Docker pendant que tu fais des tests automatisés.
donc au lieu de te connecter à une vraie base de données ou un vrai service dans tes tests, tu lances un conteneur temporaire (par exemple une base PostgreSQL, Redis, MongoDB…) juste pour la durée du test.
2-2
On en a besoin d'utiliser des variables sécurisées car il est important uqe les autres developpeurs n'aient pas acces a nos identifiants docker hub donc on les 'cache' dans des repository secrets
2-3
On a ajouté needs: build-and-test-backend pour que le job qui construit et pousse l'image Docker attende que les tests du backend soient passés avec succès avant de se lancer.
En gros, ça sert à éviter de builder une image si le code est cassé ou si les tests ne passent pas. C’est un peu comme dire : "OK, je ne construis et publie l’image que si tout est bon avant."

J’ai essayé de retirer le "needs:" pour voir, et le job de build s’est quand même lancé, même si les tests avaient échoué, donc ça peut être dangereux parce qu’on pourrait se retrouver à pousser une image qui ne fonctionne pas.

1-4

On pousse des images Docker pour pouvoir les réutiliser facilement ailleurs, comme en production, sur un serveur distant, dans un cluster Kubernetes ou même dans un autre projet.
Le but, c’est d’avoir une version figée et prête à l’emploi de notre application (backend, base de données, etc.), qu’on peut déployer n’importe où, sans se soucier de la config locale.


#TP3

3.1 - Mon fichier d'inventare est situé dans : my-project/ansible/inventories/setup.yml

Structure du fichier setup.yml
Dans ce fichier, on décrit notre serveur comme ça :

On indique l’adresse du serveur, ici c’est celine-bernardinomendes-formation.takima.cloud (c’est l’hôte)

On précise l’utilisateur avec lequel on va se connecter, ici c’est admin

On met aussi le chemin vers la clé privée qu’on utilise pour se connecter en SSH (c’est super important pour que Ansible puisse accéder au serveur sans mot de passe)

Quelques commandes de base que j’ai testées :
Pour vérifier qu'on peux bien parler avec le serveur, j’ai fait :

```
ansible all -i inventories/setup.yml -m ping
```
Ça envoie un “ping” via Ansible, et si ça répond( pong) c’est que la connexion marche

Pour récupérer des infos sur la machine distante, comme la distribution Linux, j’ai lancé :

```
ansible all -i inventories/setup.yml -m setup -a "filter=ansible_distribution*"
```
Ça nous donne des détails sur le système, ce qui peut aider pour des configurations spécifiques.

Et si on veut virer un paquet, par exemple Apache2 qu'on avait installé avant , on fait :

```
ansible all -i inventories/setup.yml -m apt -a "name=apache2 state=absent" --become
```
Là, Ansible supprime Apache2 du serveur en demandant les droits admin (--become), et si je relance la commande après, il me dira que rien n’a changé parce que c’est déjà supprimé.

3-2
On créé un rôle Ansible pour Docker avec la commande ansible-galaxy init roles/docker, puis déplacé toutes les tâches d’installation dans roles/docker/tasks/main.yml. Ensuite, on peut simplifié le playbook principal (playbook.yml) pour qu’il appelle directement ce rôle. Cela rend le projet plus propre et facile à maintenir.
```
- hosts: all
  gather_facts: true
  become: true

  roles:
    - docker
```
Ce playbook permet d'exécuter un rôle Ansible appelé docker sur tous les hôtes définis dans l'inventaire.

hosts: all
Cible tous les serveurs listés dans mon inventaire Ansible (ici, le groupe all, qui contient mon serveur distant).

gather_facts: true
Active la récupération automatique d’informations système (comme la version de l’OS, les interfaces réseau, etc.) pour pouvoir les utiliser dans les tâches.

become: true 
 Permet d’exécuter les tâches avec les droits administrateur (sudo) sur le serveur distant

roles: Liste des rôles à appliquer. Ici, on applique le rôle docker, qui contient tout ce qu’il faut pour installer et configurer Docker

3.3

On crée  les "roles" en faisant la comande suivante :

ansible-galaxy init roles/<nom du container>
On utilise le module docker_container pour dire à Ansible de lancer nos conteneurs Docker : la base de données, l’API et le proxy. Chaque conteneur est comme une boîte séparée, et Ansible s’occupe de les démarrer, les configurer, et les connecter au bon réseau

name: nom du conteneur Docker 
image: image utilisée depuis Docker Hub
state: started: on veut que le conteneur soit en cours d’exécution
restart_policy: always: si le conteneur s’arrête, Docker le redémarre automatiquement
ports: on connecte le port local 5432 à celui du conteneur
networks: ici on connecte à un réseau Docker (important pour la communication entre conteneurs)


Is it really safe to deploy automatically every new image on the hub ? explain. What can I do to make it more secure?
Non, il n’est pas totalement sûr de déployer automatiquement chaque nouvelle image dès qu’elle est poussée sur Docker Hub. Même si cela permet un déploiement rapide, cela peut introduire plusieurs risques importants.
Par exemple, une image peut être déployée alors qu’elle contient des bugs ou qu’elle n’a pas été suffisamment testée. Si un développeur pousse par erreur une image incomplète, de test, ou contenant une faille de sécurité, elle ira directement en production sans aucune validation. Cela peut provoquer des pannes, des vulnérabilités ou un comportement inattendu de l’application.
Pour rendre ce processus plus sécurisé, on peut mettre en place plusieurs mesure :
Ajouter des tests automatiques dans le pipeline (tests unitaires, d’intégration, etc..) pour s’assurer que tout fonctionne avant le déploiement
Éviter le tag latest et utiliser des tags versionnés (comme v1.0.0) afin de savoir précisément quelle version est déployée
Insérer une étape de validation manuelle pour approuver certaines mises en production sensibles
Signer les images Docker avec des outils comme Docker Content Trust pour garantir leur intégrité
Scanner les images avec des outils comme Trivy afin de détecter des failles de sécurité
Limiter les droits sur le dépôt Docker Hub : seuls certains utilisateurs devraient pouvoir pousser des images de production.


