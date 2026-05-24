# Refactoring Report

## Overview

Starting point: a single 541-line `Main` class, all game state as static
fields, all console I/O inline, the legal-play rule duplicated three times,
and cards represented as raw `String` codes. The result is six focused
classes, 100 characterization checks, and behavior verified identical via
seeded games across every step.

| File              | Lines | Role                                       |
| ----------------- | ----: | ------------------------------------------ |
| `Main.java`       |   380 | Controller (CLI, turn loop, bot, rules)    |
| `Card.java`       |   113 | Card value object (rank/color/points/effect) |
| `CardColor.java`  |    10 | Enum for the four card colors (R Y G B)    |
| `CardEffect.java` |    82 | Polymorphic per-rank behavior              |
| `PlayContext.java`|    35 | Play-target value object                   |
| `GameState.java`  |   120 | Game state + pure state ops                |
| `ConsoleView.java`|   117 | All console I/O                            |

## 1. What behavior did I characterize before refactoring?

I expanded the original 9-assertion `selfTest()` into a `CharacterizationTests`
class with **100 assertions** across ten sections, run by `scripts/test.sh`.
These are characterization tests in the strict sense: they describe what the
implementation does, including its quirks, not what ideal UNO ought to do.

Sections:

1. **Card helpers** — every branch of `color`, `rank`, `number`, `points`,
   including the `-1` sentinel for non-number cards and `CardColor.NONE` for
   wilds.
2. **Legal-play rules** — one positive and one negative test for every
   legality branch: color, number, action-type, wild, called-color.
3. **Deck behavior** — top-of-deck draw, discard-pile reshuffle, and the
   `"W"` sentinel returned when both piles are empty.
4. **Turn advancement** — forward, reverse, wrap-around, and the documented
   2-player REVERSE-as-SKIP quirk.
5. **Bot card priority** — DRAW_TWO > SKIP > NUMBER > WILD ordering.
6. **Bot color tie-break** — the implicit R > Y > G > B order.
7. **Scoring math** — point totals at the hand level.
8. **Full seeded game** — two seeds drive `playGame` end-to-end; asserts
   one winner, positive score, winner score equals sum of opponents' points,
   and up-card is not a naked wild at game end.
9. **Documented quirks** — bot-never-picks-REVERSE bug, number cross-match
   edges, empty-deck sentinel, and the dead-code penalty branch.
10. **Card effects** — direct unit tests on each `CardEffect` implementation,
    the dispatcher, and `Card.effect()`.

## 2. What were the worst design problems I found?

Ranked by refactor-blocking danger:

1. **Triple-duplicated legal-play rule.** The same five-branch legality check
   appeared inline in `playGame`, inline three times in `chooseBotCard`, and
   as the standalone `isLegal` method. Any rule change silently diverged.
2. **God method `playGame`** (192 lines, four-level nesting). Deck
   construction, dealing, prompts, bot strategy, legality, card effects, win
   detection, and scoring were all tangled together.
3. **Switch-on-rank-string for card effects.** A 31-line `if/else if` chain
   with no extension point.
4. **Raw `String` cards everywhere.** Card behavior (`color`, `rank`,
   `number`, `points`) was scattered across static helpers. `ArrayList<String>`
   hands and decks offered no type safety and required string-parsing on every
   access.
5. **No color type.** The called color after a wild was an unvalidated `String`
   that could silently be any value.
6. **Global mutable state.** Thirteen static fields on `Main`, all reachable
   from anywhere.
7. **Console I/O woven through rule code.** `System.out` and `Scanner` calls
   inside the turn loop, the bot draw branch, and the win-tally branch made
   rule logic impossible to test without a terminal.

## 3. Which refactorings did I perform?

Eight steps in order. After every step all characterization checks passed and
seeded scoreboards were unchanged.

### Step 1 — Collapse the duplicated legal-play rule

Replaced the inline 27-line `ok`-flag block in `playGame` and the three
copy-pasted `ok` blocks in `chooseBotCard` with single calls to the existing
`isLegal` method. `isLegal` became the single source of truth.

### Step 2 — Introduce a `Card` value object

Moved `color`, `rank`, `number`, `points` onto an immutable `Card` value
object. Static helpers on `Main` became one-line adapters delegating to
`Card.of(code).method()`.

### Step 3 — Extract `ConsoleView`

Every `System.out` write and `Scanner.nextLine()` read moved into
`ConsoleView`. Each method honors `Main.quiet` identically to the previous
`if (!quiet)` guards.

### Step 4 — Extract `GameState`

All thirteen pieces of game state moved off `Main` into a `GameState`
instance. Pure state operations (`next`, `draw`, `shuffleDeck`,
`buildStandardDeck`, `dealStartingHands`, `flipInitialUpCard`) moved with
them. `Main` now owns three fields: `state`, `quiet`, `view`.

### Step 5 — `CardEffect` polymorphism

Replaced the 26-line `if/else if` card-effect chain with a single
`card.effect().apply(state, view)` call. `CardEffect` is an interface with
five package-private implementations and a `forRank` dispatcher. Adding a
new card type requires one new class and one line in `forRank`.

### Step 6 — Split `playGame`

`playGame` shrank from ~104 lines to two lines. Setup moved into
`startNewRound`, the guard loop into `runTurnLoop`, and one turn into
`playSingleTurn`, which delegates to six focused helpers:
`pickCardOrDraw`, `isValidPlay`, `applyPlay`, `handleWinIfAny`,
`sumOpponentPoints`, `isCurrentHuman`.

### Step 7 — Introduce `PlayContext`

The `(upCard, calledColor)` pair was passed as two separate arguments to
seven call sites. `PlayContext(Card upCard, CardColor calledColor)` bundles
them. `isLegal(card, up, call)` became `isLegal(card, context)`.

### Step 8 — Migrate card storage to `ArrayList<Card>` + `CardColor` enum

Introduced `CardColor` (R, Y, G, B, NONE) for the called-color field and
for `Card.color()`. Migrated `GameState.deck`, `discard`, `hands`, and
`upCard` from `String` to `Card`. Removed the `Main.color/rank/number/points`
string adapter methods. `chooseBotColor` and `askColor` now return
`CardColor`; `calledColor` is `CardColor.NONE` when no wild is in effect.

## 4. What behavior did I intentionally preserve?

All behaviors listed in `docs/rules.html` and the midterm brief as documented
quirks:

1. **All hands visible in the terminal** on every turn.
2. **Humans can type `draw` while holding a legal play.**
3. **Illegal-index penalty branch** preserved verbatim (dead code, characterized
   in Section 9).
4. **Bots auto-play a legal drawn card.**
5. **Bot never picks a legal REVERSE** — `chooseBotCard` covers only DRAW_TWO,
   SKIP, NUMBER, and WILD. Characterized as `BUG-CHARACTERIZATION` in Section 9.
6. **2-player REVERSE acts as SKIP** — `ReverseEffect` preserves the
   `playerNames.size() == 2` branch.
7. **Initial wild up-card is redrawn** — `flipInitialUpCard` loops while
   `upCard.isWild()`.
8. **`draw()` returns the `"W"` sentinel** when both piles are empty.
9. **Bot color tie-break R > Y > G > B** — the chained `>=` comparisons in
   `chooseBotColor` were preserved.
10. **All CLI output strings byte-identical** — verified by running the same
    seeded games before and after each step.

## 5. What risks remain?

1. **`chooseBotCard` does not consider REVERSE.** A bot holding only a legal
   reverse draws instead of playing. Documented and characterized; fixing it
   is a deliberate scope decision, not an oversight.
2. **`rank()` still returns a `String`.** Moving it to an enum (like
   `CardColor`) would eliminate the `rank().equals("DRAW_TWO")` string
   comparisons, but that step was not done here.
3. **Dead-code penalty branch.** The `chosen >= hand.size()` check in
   `isValidPlay` is unreachable from the current `askHuman` and
   `chooseBotCard`, which only return valid indices or `-1`.
4. **Tests reach into package-private state** via `Main.state.X`. Convenient
   for characterization, but would break if `GameState` fields were
   privatized.
5. **`Main.selfTest()` holds a reference to `CharacterizationTests.run()`.**
   Production code calling test code is a mild coupling. The clean fix is to
   give `CharacterizationTests` its own `main(args)` and repoint
   `scripts/test.sh` at it directly.
6. **`scores` is a fixed `int[10]`.** Safe for 2–4 players but unchecked.
