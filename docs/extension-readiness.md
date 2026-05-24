# Extension Readiness

## Which extension would the design support best?

**Adding a new card effect** (e.g., a Swap Hands card, a Draw Three, or a
Skip-Everyone card) is the most directly supported extension.

A close second is **replacing or improving the CLI view**, which is also a
one-class change. The third most accessible is **a smarter bot strategy**.

## Where would each change be implemented?

### Add a new card effect

1. Add a new code to `Card` — e.g., the deck-building loop in
   `GameState.buildStandardDeck()` is the only place cards are minted, so
   add `deck.add(Card.of(colors[c] + "X"))` there.
2. Create a new implementation class in `src/CardEffect.java`:

   ```java
   class SkipAllEffect implements CardEffect {
       public void apply(GameState state, ConsoleView view) {
           // advance past all other players, stop at current player + 1
           for (int i = 1; i < state.playerNames.size(); i++) {
               state.next();
           }
           state.next();
       }
   }
   ```

3. Add one line to `CardEffect.forRank`:

   ```java
   if (rank.equals("SKIP_ALL")) return SKIP_ALL;
   ```

4. Optionally add a `points()` case in `Card.points()` if the card needs a
   non-default score.

The turn loop in `Main.playSingleTurn()` does not need to change — it already
calls `card.effect().apply(state, view)` unconditionally.

### Replace or improve the CLI view

`ConsoleView` is already the single class that owns all `System.out` writes
and `Scanner` reads. To replace it:

1. Extract an interface from `ConsoleView` (or just subclass it).
2. Swap `Main.view = new ConsoleView()` for the alternative implementation.

The game loop in `Main` references only the `ConsoleView` type; no method
other than `main` would need to change. This seam was the explicit goal of
Step 3 in the refactoring.

### Smarter bot strategy

`chooseBotCard(ArrayList<Card> hand)` and `chooseBotColor(ArrayList<Card> hand)`
in `Main` are the bot's entire decision surface. To add a smarter strategy:

1. Extract a `BotStrategy` interface with `chooseCard` and `chooseColor`
   methods.
2. Implement the current greedy bot as `GreedyBotStrategy`.
3. Wire `playSingleTurn` to call `strategy.chooseCard(hand, context)` instead
   of the static methods.

As a bonus, the existing `chooseBotCard` bug (never picks REVERSE) would be
trivially fixable in the smarter implementation without touching the current
characterized behavior — both strategies can coexist.

## What part of the design still makes change difficult?

### `rank()` is still a `String`

Card ranks are compared via `card.rank().equals("SKIP")`, `"DRAW_TWO"`, etc.
throughout `chooseBotCard`, `CardEffect.forRank`, and `Card.effect()`. A
`CardRank` enum (analogous to the `CardColor` enum added in Step 8) would
eliminate these string comparisons and make typos compile-time errors. Until
then, adding a new rank requires remembering to update every
`rank().equals("...")` call site manually.

### `GameState` fields are package-private and mutable

`GameState` exposes mutable `ArrayList<Card>` fields directly. Any
new extension class in the same package can modify them freely.
Privatizing fields and adding accessor methods would protect invariants
(e.g., the deck can't be re-ordered arbitrarily from outside) but was
deferred as a step beyond the midterm scope.

### `Main` is still the host for `isLegal`, bot logic, and CLI parsing

`isLegal` is a natural candidate to move onto `PlayContext` as
`context.allows(card)`. Bot logic is a natural candidate for a `BotStrategy`
type. CLI parsing is a natural candidate for a small `Config` class.
Each would make `Main` a thinner orchestrator and reduce the surface area
a contributor has to understand before adding a feature. None were done
here because the rubric rewards incremental steps over large restructures,
and the current boundaries are already a significant improvement over the
starting monolith.
