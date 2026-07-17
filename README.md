# LogisticsConnect

## Overview

Supply chain parcel delivery hub and transit delay tracking.

Domain entities: hubs, sorting centers, regional districts.

Every class in this repo lives in a single flat package, `co.wethinkcode.logisticsconnect`. LogisticsConnect is built
as a small set of independent services, following a growth path from simple data
cleanup through synchronous REST calls to asynchronous MQ decoupling and alerting:

1. clean a messy legacy CSV export (`hubs-global.csv`) — handled by **IngestionServiceApp**
2. serve it up and act on it, via three REST services calling each other directly
   over HTTP
3. decouple the relevant services with an ActiveMQ topic (`package-status-topic`) instead of
   direct calls — shared broker setup lives in [`common/`](common)
4. raise the alarm on failure — handled by **AlertBotApp**

| Service | Folder | Port | Role |
|---|---|---|---|
| IngestionServiceApp | [`ingestion-service/`](ingestion-service) | 7050 | Parses and cleans `hubs-global.csv` |
| HubServiceApp | [`hub-service/`](hub-service) | 7051 | Serves provinces and sorting centers (place-name source of truth). |
| DelayStageServiceApp | [`delay-stage-service/`](delay-stage-service) | 7052 | Tracks the Transit Delay Stage (0-8, e.g. weather shutdowns). |
| TransitServiceApp | [`transit-service/`](transit-service) | 7053 | Calculates estimated arrival windows based on hub and delay stage. |
| AlertBotApp | [`alertbot/`](alertbot) | 7054 | posts proactive delay notifications to public transit social media pages (simulated). |

Plus [`common/`](common) (no port) — the shared ActiveMQ broker and MQ config notes
for `package-status-topic`: Package status updates move from latency-driven RPC to bandwidth-driven messaging.

**Status:** scaffold only — build files, Javalin bootstrap, and TODOs are in place; no
business logic has been implemented yet.

## Project structure

```
logisticsconnect/
├── README.md
├── .gitignore
├── ingestion-service/          (port 7050)
│   ├── pom.xml
│   ├── README.md
│   └── src/main/
│       ├── java/co/wethinkcode/logisticsconnect/IngestionServiceApp.java
│       └── resources/hubs-global.csv
├── hub-service/          (port 7051)
├── delay-stage-service/          (port 7052)
├── transit-service/          (port 7053)
├── common/
│   ├── docker-compose.yml
│   └── README.md
└── alertbot/          (port 7054)
```

## Build

Requirements: Java 17+, Maven 3.8+, Docker (for the broker in `common/`).

Every folder here (`ingestion-service/`, each domain service, and `alertbot/`) is
an **independent** Maven project — there is no parent/aggregator pom. Build one at a
time, e.g.:

```
cd hub-service
mvn package
```

...or build every module in the repo in one pass from the project root:

```
find . -name pom.xml -execdir mvn -q package \;
```

## Run

```
# ingestion
cd ingestion-service && mvn package && java -jar target/ingestion-service.jar

# domain services, each in its own terminal
# terminal 1
cd hub-service && mvn package && java -jar target/hub-service.jar
# terminal 2
cd delay-stage-service && mvn package && java -jar target/delay-stage-service.jar
# terminal 3
cd transit-service && mvn package && java -jar target/transit-service.jar

# MQ broker (needed once the MQ-aware services above are wired up)
cd common && docker compose up -d

# alerting
cd alertbot && mvn package && java -jar target/alertbot.jar
```

| Service | Port |
|---|---|
| IngestionServiceApp (`ingestion-service`) | 7050 |
| HubServiceApp (`hub-service`) | 7051 |
| DelayStageServiceApp (`delay-stage-service`) | 7052 |
| TransitServiceApp (`transit-service`) | 7053 |
| AlertBotApp (`alertbot`) | 7054 |

## Test

No automated tests exist yet (this is a scaffold). Each running service exposes
`/health`, so sanity-check manually:

```
curl http://localhost:7050/health   # -> OK
```

To add real tests to a module, add JUnit 5 and Surefire to its `pom.xml`:

```xml
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>5.10.2</version>
  <scope>test</scope>
</dependency>
```

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.2.5</version>
</plugin>
```

then add tests under that module's `src/test/java/...` and run:

```
mvn test
```
