# Final Report — UNO CLI

## 1. Overview

This project turns the earlier midterm UNO codebase into a fuller, testable UNO
product: a command-line game for 2–4 human and/or computer players, with the
game rules separated from the console so they can be exercised by automated
tests. This report summarizes the rules implemented, how the game is played,
how the code is organized, the tests added, and the remaining limitations.

## 2. UNO rules implemented

All ten rule areas from the feature menu are implemented:

| Area | Notes |
|------|-------|
| Deck composition | Standard 108-card deck. |
| Legal play validation | Color / number / action / wild; called color after a wild. |
| Skip | Next player loses their turn. |
| Reverse | Flips direction; acts as Skip in a 2-player game. |
| Draw Two | Next player draws two and is skipped (no stacking). |
| Wild | Player chooses the active color; it drives legality. |
| Wild Draw Four | Chooses color; next player draws four and is skipped (no challenge). |
| Draw / pass | Draw one; play it if legal, otherwise pass. |
| UNO call & penalty | One-card detection, call, two-card missed-call penalty. |
| Round scoring & multi-round target | Round scoring; match played to a target score (default 500). |

Exact behavior, variants, and simplifications are documented in
[`rules-supported.md`](rules-supported.md).

## 3. Playing from the CLI

Run a match with Maven (or `scripts/run.sh`):

```bash
mvn -q compile exec:java -Dexec.mainClass="uno.Main" -Dexec.args="--human --bots 2"
```

Options: `--human`, `--bots N`, `--target N`, `--games N`, `--seed N`,
`--quiet`, `--self-test`, `--help` (see the README for the full table).

Each turn prints the up-card, the called color if any, and the current player's
hand as an indexed list. A human plays by entering an index or a card code
(e.g. `2` or `G+2`), or `DRAW` to draw. After a wild the player is asked for a
color; on reaching one card the player is asked whether to call UNO, and a
declined call costs two penalty cards. Bad input (unknown card, illegal play,
non-numeric option) is rejected without crashing and the player is re-prompted.

A **match** is a series of **rounds**. Each round is a hand of UNO that ends when
a player empties their hand and scores the value of the other hands. Rounds
repeat until a player reaches the target score, and the highest scorer wins the
match. The final scoreboard and match winner are always printed, even under
`--quiet`.

## 4. Architecture: separating game logic from the CLI

The core design goal was that **rules do not live in the console**. Each class
has a single clear responsibility:

- **`Card` / `CardColor` / `PlayContext`** — immutable value objects. A card
  knows its color, rank, number, and point value; `PlayContext` bundles the
  up-card and called color that legality depends on.
- **`Rules`** — pure functions: `isLegal(card, context)` and the scoring
  helpers `scoreForWinner` / `handValue`. No state, no I/O.
- **`CardEffect`** — the polymorphic "what happens after this card is played"
  strategy (Skip, Reverse, Draw Two, Wild Draw Four, Normal). Turn advancement
  and forced draws are selected by card type instead of an `if`/`else` chain.
- **`BotStrategy`** — computer-player decisions (which card, which color,
  whether to call UNO), as pure functions of the hand and context.
- **`GameState`** — all mutable data (hands, deck, discard, scores, direction,
  UNO flags) and deck operations (build, shuffle, deal, draw, flip).
- **`ConsoleView`** — the *only* class that reads stdin or writes stdout.
- **`Main`** — the controller: parses arguments and runs the match/round/turn
  loops, delegating every rule decision to the classes above.

Because `Rules`, `CardEffect`, `BotStrategy`, and `GameState` never touch the
console, a full bots-only game runs headlessly, and every rule can be tested by
constructing state directly and asserting on the result. `ConsoleView`
announcements honor `--quiet`, which is what lets tests run a whole match
silently.

## 5. Tests added

The project uses JUnit 5 (run with `mvn test`). Tests are organized one file per
rule area so the mapping to the rubric is explicit:

| Test class | Covers |
|------------|--------|
| `DeckCompositionTest` | 108 cards, per-color counts, four of each wild. |
| `LegalPlayTest` | Color/number/action matches, wilds, called color, rejections. |
| `ActionCardsTest` | Skip, Reverse (3+ and 2-player), Draw Two effects. |
| `WildCardsTest` | Wild color choice affecting legality; Wild Draw Four. |
| `DrawPassTest` | Draw-and-play-if-legal, and pass when not. |
| `UnoCallPenaltyTest` | One-card detection, call, two-card missed-call penalty. |
| `ScoringTest` | Card values, opponent-hand totals, a scored round. |
| `MatchTargetTest` | Fixed-round and play-to-target matches, final winner. |
| `BotStrategyTest` | Bot play priority (incl. the Reverse fix) and color ties. |

These sit alongside the retained `CharacterizationTests` (100 checks pinning the
original behavior, run through `CharacterizationTestsTest`) and the persistence
DAO tests. The full suite is **58 test methods, all passing**. A built-in
`--self-test` mode also runs the characterization checks from the executable.

One behavior was intentionally changed and re-pinned: bots previously never
played a legal Reverse and drew instead. `BotStrategy` now plays it, and the
characterization check for that case was updated to assert the fixed behavior.

## 6. Limitations

- **No stacking** of Draw Two / Wild Draw Four cards.
- **No Wild Draw Four challenge**, and no restriction on when it may be played.
- **Missed-UNO timing is immediate**: a player is caught the instant they fail
  to call on reaching one card, rather than allowing another player to catch
  them later. Bots always call, so in bots-only games the penalty never fires;
  it is reachable through human play and is covered by unit tests.
- **Simple bot AI**: fixed-priority heuristic, no look-ahead or card counting.
- **Index-based human input is not pre-validated** for legality: entering the
  index of an illegal card incurs the standard draw-a-penalty-card branch,
  whereas entering it by code is rejected up front and re-prompted.
- **Persistence is best-effort**: a database failure is logged and skipped
  rather than surfaced interactively.
