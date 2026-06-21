# Database Documentation

## Selected Database
I chose the H2 database for its simplicity and suitability for local development and testing. It is a lightweight, in-memory capable database that requires no external server setup for basic usage.

## Selected ORM/Persistence Framework
I used Hibernate ORM with Jakarta Persistence for object-relational mapping.

## Schema Setup
The database schema is automatically managed by Hibernate using the `hbm2ddl.auto=update` setting in `hibernate.cfg.xml`. On application startup, Hibernate will create the necessary tables if they do not exist, or update them if the schema has changed.

### Tables
The schema consists of three tables corresponding to the following entities:

1. **Player**
   - Columns: `id` (primary key, auto-increment), `name` (varchar)

2. **Game** (representing a match of one or more rounds)
   - Columns: `id` (primary key, auto-increment), `start_time` (timestamp), `end_time` (timestamp), `winner_id` (foreign key to Player.id), `round_count` (integer)

3. **GameScore**
   - Columns: `id` (primary key, auto-increment), `game_id` (foreign key to Game.id), `player_id` (foreign key to Player.id), `points` (integer)

### Hibernate Configuration
The Hibernate configuration file is located at `src/main/resources/hibernate.cfg.xml`. It specifies:
- Database connection: JDBC URL for H2 (`jdbc:h2:~/uno;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`)
- Dialect: `org.hibernate.dialect.H2Dialect`
- Credentials: username `sa`, empty password
- SQL logging: enabled for debugging
- Automatic schema updates: `hbm2ddl.auto=update`

## Running Persistence Tests
Persistence tests are located in the `src/test/java` directory. They use an in-memory H2 database to isolate tests from the development database.

To run the tests:
```bash
mvn test
```

The test suite includes:
- PlayerDaoTest: tests CRUD operations for Player
- GameDaoTest: tests saving games and scores, and retrieving recent games
- GameScoreDaoTest: tests retrieving scores by game

## Viewing Game History and Statistics
The application provides the following query features through the DAO layer:

1. **List recent games**: `GameDao.findRecentGames(int limit)` returns the most recent matches.
2. **Show player win count**: Can be derived by querying games where a player is the winner.
3. **Show highest scores**: Can be derived by querying GameScore entries and summing points per player across matches.

These methods are used in the persistence tests and can be invoked from the main application if extended to a CLI menu.

## Example Usage
After playing one or more matches, the game history is stored in the database. To inspect the database, you can use the H2 console:
```bash
java -cp ~/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar org.h2.tools.Console
```
Then connect to the JDBC URL `jdbc:h2:~/uno` with username `sa` and no password.

