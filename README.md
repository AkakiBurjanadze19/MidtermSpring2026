# UNO CLI

A command-line implementation of UNO for 2–4 players (human and/or computer
players), with fuller UNO rules, a rule layer that is testable without the
console, and optional persistence of match history.

## Quick start

```bash
# Watch three bots play a full match to 500 points
mvn -q compile exec:java -Dexec.mainClass="uno.Main" -Dexec.args="--bots 3"

# Play yourself against two bots
mvn -q compile exec:java -Dexec.mainClass="uno.Main" -Dexec.args="--human --bots 2"
```

Equivalent convenience scripts (they just call Maven):

```bash
scripts/run.sh --human --bots 2      # compile + run
scripts/test.sh                      # run the test suite
```

## Command-line options

| Option        | Meaning                                                        |
|---------------|----------------------------------------------------------------|
| `--human`     | Add a human player named "You" (default: bots only).           |
| `--bots N`    | Number of computer players (default: 3).                       |
| `--target N`  | Play rounds until a player reaches N points (default: 500).    |
| `--games N`   | Instead play exactly N rounds (a fixed-length match).          |
| `--seed N`    | Seed the shuffler for reproducible games.                      |
| `--quiet`     | Suppress per-turn narration (keeps standings and final result).|
| `--self-test` | Run the built-in characterization checks and exit.             |
| `--help`      | Print usage.                                                   |

Total players (human + bots) must be between **2 and 4**.

## How to play

On your turn the game shows the up-card, any called color, and your hand as an
indexed list, e.g. `0:R5 1:G+2 2:W`. At the prompt you can:

- type an **index** (`1`) or a **card code** (`G+2`) to play that card;
- type **`DRAW`** to draw one card. If the drawn card is playable you are asked
  whether to play it; otherwise your turn passes.

Card codes: colors are `R Y G B`; numbers `0`–`9`; `S` = Skip, `R` = Reverse,
`+2` = Draw Two; `W` = Wild, `W4` = Wild Draw Four (for example `RS`, `B+2`,
`W4`). After a wild you are prompted for the color to call (`R/Y/G/B`).

When you play your second-to-last card you are asked **"Call UNO?"**. If you
decline, you are immediately caught and draw two penalty cards.

Invalid input (a bad index, an unknown card, an illegal play, a non-number for a
numeric option) is handled without crashing — you are prompted again or shown a
usage message.

## Rules implemented

Correct 108-card deck, legal-play validation (color / number / action / wild),
Skip, Reverse (acts as Skip with two players), Draw Two, Wild, Wild Draw Four,
draw-one-then-play-or-pass, UNO call with a missed-call penalty, round scoring,
and a multi-round match played to a target score. See
[`docs/rules-supported.md`](docs/rules-supported.md) for the exact behavior and
the variants/simplifications chosen, and [`docs/final-report.md`](docs/final-report.md)
for the full write-up.

## Architecture

The rules do not live in the CLI. Responsibilities are split so game logic can
be tested without any console input:

| Class            | Responsibility                                            |
|------------------|-----------------------------------------------------------|
| `Card`           | Immutable card value object (color, rank, number, points).|
| `CardColor`      | The four colors plus `NONE` for wilds.                    |
| `Rules`          | Legal-play validation and round scoring (pure functions). |
| `CardEffect`     | The state change each played card causes (Skip/Reverse/…).|
| `BotStrategy`    | Computer-player card/color/UNO decisions (pure).          |
| `GameState`      | All mutable state and deck operations (draw, deal, build).|
| `ConsoleView`    | Every read from stdin and write to stdout.                |
| `Main`           | CLI parsing and the match/round/turn orchestration loops. |

`PlayContext` bundles the current up-card and called color that the legality
rule reads.

## Build, test, package

```bash
mvn compile          # compile
mvn test             # run all JUnit tests
mvn package          # build target/uno-cli-1.0.0.jar (main class uno.Main)
```

Requires JDK 25 and Maven. No other setup is needed; the H2 database file is
created automatically under your home directory on first run.

## Persistence (optional feature)

At the end of a match the players, match metadata (start/end time, rounds,
winner), and per-player scores are stored via Hibernate ORM in an embedded H2
database (`~/uno.mv.db`). See [`docs/database.md`](docs/database.md) for the
schema. Persistence is best-effort: if the database is unavailable the match
still completes and a warning is logged.

## Logging

The game logs key events (game start, turns, cards played/drawn, invalid input,
round/game end) via SLF4J at INFO level to stderr. Third-party (Hibernate)
logging is raised to WARN so it does not clutter the player-facing output.

## Docker

```bash
docker build -t uno-cli .
docker run --rm uno-cli --bots 3
```
