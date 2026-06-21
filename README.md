# UNO CLI

This is a command-line UNO game with persistence of game history and player statistics.

## Build Tool

This project uses Maven as its build tool.

## Persistence

The application uses Hibernate ORM with an H2 database to persist game results. At the end of each match (series of rounds), the following is stored:
- Players
- Match (game) details: start/end timestamps, winner, number of rounds
- Per-player scores for the match

### Database Schema
The schema consists of three tables: `Player`, `Game`, and `GameScore`. See `docs/database.md` for detailed schema information.

### Configuration
Hibernate is configured via `src/main/resources/hibernate.cfg.xml`. The database file is created in the user's home directory as `uno.mv.db`.

### Query Features
The persistence layer supports:
- Listing recent games
- Querying player win counts
- Retrieving highest scores

These features are tested in the persistence test suite.

## Commands

### Local Build

Compile the project:
```bash
mvn compile
```

### Local Test

Run the tests:
```bash
mvn test
```

### Local Run

Run the application (example with 3 bots and 1 human player, 1 game):
```bash
mvn exec:java -Dexec.mainClass="uno.Main" -Dexec.args="--human --bots 2 --games 1"
```

### Package Creation

Create an executable JAR:
```bash
mvn package
```
The JAR will be placed in `target/uno-cli-1.0.0.jar`.

### Docker Build

Build the Docker image:
```bash
docker build -t uno-cli .
```

### Docker Run

Run the Docker container (example with 3 bots and 1 human player, 1 game):
```bash
docker run --rm uno-cli --human --bots 2 --games 1
```

## Logging

The application uses slf4j logging library for logging important game events:
- Game start
- Player turn
- Card played
- Card drawn
- Invalid input
- Round or game end

Logs are printed to the console with the INFO level.

## Notes

- The CLI should still be readable for players; logging does not replace normal user-facing output.
- The Docker build uses the official Eclipse Temurin JDK 25 image.
- The application is packaged as an executable JAR with the main class set to `uno.Main`.

