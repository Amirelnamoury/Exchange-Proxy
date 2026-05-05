# Exchange Proxy - Application temps reel

## Important : migration Spring Boot

Le projet a ete migre de **Python** vers **Java Spring Boot**.

Il ne faut donc plus lancer :

```bash
python producer/main.py
python consumer/main.py
pip install -r requirements.txt
mvn -pl producer exec:java
mvn -pl consumer exec:java
```

Le producer et le consumer sont maintenant reunis dans **une seule application Spring Boot**.

## Objectif du projet

Exchange Proxy centralise les appels a l'API de taux de change pour eviter que plusieurs equipes interrogent directement le fournisseur.

Le pipeline fait ceci :

1. Un service Spring planifie appelle l'API `https://api.exchangerate-api.com/v4/latest/USD`.
2. Il publie le JSON dans Kafka sur le topic `exchange-rates`.
3. Un consumer Spring Kafka lit ce meme topic.
4. Les donnees sont indexees dans Elasticsearch dans l'index `forex-data`.
5. Kibana permet de creer un tableau de bord de visualisation.

## Architecture

```text
API Exchange Rate
        |
        v
Spring Boot Scheduler
        |
        v
Spring Kafka Producer
        |
        v
Kafka topic: exchange-rates
        |
        v
Spring Kafka Consumer
        |
        v
Elasticsearch index: forex-data
        |
        v
Kibana
```

## Prerequis

- Docker Desktop
- Java JDK 17
- Maven

Verifier :

```powershell
java -version
mvn -version
docker version
```

## Installation

```powershell
git clone https://github.com/yahzo/Exchange-Proxy.git
cd Exchange-Proxy
git checkout step-2
mvn clean package
```

## Lancement

### 1. Demarrer l'infrastructure

Ouvrir Docker Desktop, puis lancer :

```powershell
docker compose up -d
```

Verifier :

```powershell
docker compose ps
```

Services attendus :

- Kafka : `localhost:9092`
- Elasticsearch : `http://localhost:9200`
- Kibana : `http://localhost:5601`

### 2. Lancer l'application Spring Boot

```powershell
mvn spring-boot:run
```

L'application :

- cree le topic Kafka `exchange-rates` si besoin ;
- appelle l'API toutes les 60 secondes ;
- publie les taux dans Kafka ;
- consomme les messages Kafka ;
- indexe les documents dans Elasticsearch.

### 3. Verifier l'application

Dans le navigateur :

```text
http://localhost:8080
```

Endpoint de sante Spring Boot :

```text
http://localhost:8080/actuator/health
```

### 4. Ouvrir Kibana

```text
http://localhost:5601
```

Creer une Data View :

```text
Index pattern: forex-data
Time field: @timestamp
```

## Configuration

Les valeurs sont dans `src/main/resources/application.properties`.

| Propriete | Defaut | Description |
| --- | --- | --- |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Broker Kafka |
| `spring.kafka.consumer.group-id` | `exchange-proxy-consumer` | Groupe Kafka du consumer |
| `exchange.kafka.topic` | `exchange-rates` | Topic Kafka |
| `exchange.kafka.partitions` | `3` | Nombre de partitions du topic |
| `exchange.kafka.replication-factor` | `1` | Replication factor local |
| `exchange.api-url` | `https://api.exchangerate-api.com/v4/latest/USD` | API de taux de change |
| `exchange.fetch-interval-ms` | `60000` | Intervalle d'appel API |
| `exchange.elasticsearch.url` | `http://localhost:9200` | Adresse Elasticsearch |
| `exchange.elasticsearch.index` | `forex-data` | Index Elasticsearch |

Exemple pour accelerer les tests :

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--exchange.fetch-interval-ms=10000"
```

## Commandes utiles

Compiler :

```powershell
mvn clean package
```

Voir les logs Docker :

```powershell
docker compose logs -f
```

Voir les logs Kafka :

```powershell
docker compose logs -f kafka
```

Arreter l'infrastructure :

```powershell
docker compose down
```

## Lancement sur ce poste avec WSL

Sur ce poste, l'infrastructure Docker tourne dans Ubuntu WSL, sans Docker Desktop.

Depuis PowerShell :

```powershell
cd C:\Users\Utilisateur\Documents\application_temps_reel\Exchange-Proxy
Start-Process -FilePath C:\Windows\System32\wsl.exe -ArgumentList @("-d","Ubuntu","-u","root","--exec","sleep","infinity") -WindowStyle Hidden
wsl -d Ubuntu -u root -- bash -lc "cd /mnt/c/Users/Utilisateur/Documents/application_temps_reel/Exchange-Proxy && docker compose up -d"
```

La commande `sleep infinity` garde WSL actif pendant que les conteneurs tournent.

Verifier les conteneurs :

```powershell
wsl -d Ubuntu -u root -- bash -lc "docker ps"
```

Lancer l'application Spring Boot avec Java 17 :

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:Path="$env:JAVA_HOME\bin;C:\Users\Utilisateur\Documents\application_temps_reel\tools\apache-maven-3.9.11\bin;$env:Path"
mvn spring-boot:run "-Dspring-boot.run.arguments=--exchange.fetch-interval-ms=10000"
```

Ouvrir ensuite :

```text
http://localhost:8080
http://localhost:8080/actuator/health
http://localhost:9200
http://localhost:5601
```

Arreter l'application Spring Boot avec `Ctrl+C`, puis arreter l'infrastructure :

```powershell
wsl -d Ubuntu -u root -- bash -lc "cd /mnt/c/Users/Utilisateur/Documents/application_temps_reel/Exchange-Proxy && docker compose down"
```

## Depannage

### `localhost:9092` ne s'ouvre pas dans le navigateur

C'est normal. Kafka n'est pas une page web HTTP.

Tester le port :

```powershell
Test-NetConnection localhost -Port 9092
```

La valeur attendue est :

```text
TcpTestSucceeded : True
```

### Kafka ou Elasticsearch ne repond pas

Verifier Docker :

```powershell
docker compose ps
```

Relancer :

```powershell
docker compose up -d
```

### Conflit de noms Docker

Si d'anciens conteneurs bloquent les noms :

```powershell
docker ps -a
docker rm elasticsearch kibana kafka-broker
docker compose up -d
```

## Structure du projet

```text
Exchange-Proxy/
|-- docker-compose.yml
|-- pom.xml
`-- src/
    `-- main/
        |-- java/com/yahzo/exchangeproxy/
        |   |-- ExchangeProxyApplication.java
        |   |-- config/
        |   |-- producer/
        |   |-- consumer/
        |   `-- web/
        `-- resources/application.properties
```

## Versions principales

- Java 17
- Spring Boot 3.4.2
- Maven 3.x
- Kafka Docker `apache/kafka:3.9.0`
- Kafka clients `3.9.0`
- Elasticsearch `8.12.0`
- Kibana `8.12.0`
- Elasticsearch Java client `8.12.0`
