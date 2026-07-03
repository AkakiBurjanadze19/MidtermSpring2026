# Rules Supported

This document maps every rule in `Final_Project_UNO_rules_reference.md` to what
this project implements, and records the variants and simplifications chosen.
Each rule notes where it lives in the code and where it is tested.

## Deck Composition 

Standard 108-card deck: four colors; one `0` and two each of `1`–`9` per color;
two Skip, Reverse and Draw Two per color; four Wild and four Wild Draw Four.

- Code: `GameState.buildStandardDeck()`
- Tests: `DeckCompositionTest`

## Legal Play Validation 

A card is legal if it is a Wild/Wild Draw Four, or its color matches the active
color, or (for action cards) its action matches the up-card's action, or (for
number cards) its number matches the up-card's number. The **active color** is
the color called by the most recent wild if one is in effect, otherwise the
up-card's own color.

- Code: `Rules.isLegal(Card, PlayContext)`, `Rules.activeColor(PlayContext)`
- Tests: `LegalPlayTest`, and `CharacterizationTests` section 2

## Skip 

The next player loses their turn; play continues with the following player.

- Code: `CardEffect.SKIP` (`SkipEffect`)
- Tests: `ActionCardsTest`

## Reverse 

With three or more players, Reverse flips the direction of play. **Two-player
variant:** with exactly two players Reverse is treated as a Skip, so the player
who played it takes another turn. This is the variant recommended by the
reference document.

- Code: `CardEffect.REVERSE` (`ReverseEffect`)
- Tests: `ActionCardsTest.reverseFlipsDirectionForThreePlusPlayers`,
  `ActionCardsTest.reverseActsLikeSkipInTwoPlayerGame`

## Draw Two 

The next player draws two cards and loses their turn; play continues with the
following player. **Simplification:** Draw Two cards are **not** stackable.

- Code: `CardEffect.DRAW_TWO` (`DrawTwoEffect`)
- Tests: `ActionCardsTest.drawTwoMakesNextPlayerDrawTwoAndLoseTurn`

## Wild 

The player who plays a Wild chooses the next active color, which then drives
legal-play validation. The next player takes a normal turn. A human is prompted
for the color; a bot calls the color it holds most of (ties broken R > Y > G > B).

- Code: color choice in `Main.applyPlay` / `Main.askColor` / `BotStrategy.chooseColor`;
  effect is `CardEffect.NORMAL`
- Tests: `WildCardsTest.chosenColorBecomesActiveAndAffectsLegality`,
  `WildCardsTest.botChoosesItsMajorityColorAfterWild`

## Wild Draw Four 

The player chooses the next active color, the next player draws four cards and
loses their turn, and play continues with the following player. **Simplification:**
the optional Wild-Draw-Four *challenge* rule is not implemented, and Wild Draw
Four may be played at any time (no "only if you have no matching color" restriction).

- Code: color choice in `Main.applyPlay`; `CardEffect.WILD_DRAW_FOUR`
- Tests: `WildCardsTest.wildDrawFourMakesNextPlayerDrawFourAndLoseTurn`

## Draw / Pass Behavior 

**Chosen variant:** a player with no legal play draws exactly one card. If that
card is legal it may be played immediately (bots always do; a human is asked
"Play drawn card? y/n"); otherwise the turn passes with the drawn card kept in
hand.

- Code: `Main.pickCardOrDraw`, `GameState.draw`
- Tests: `DrawPassTest`

## UNO Call and Missed-UNO Penalty 

The one-card state is detected the moment a player plays their second-to-last
card. **Timing rule:** the player must declare UNO immediately; a human is
prompted "Call UNO?", and a bot always calls reliably. **Penalty:** a player who
fails to call is caught right away and draws two penalty cards. (Because the
check is immediate, bots — which always call — are never penalized; the penalty
path is driven by human play and covered directly by tests.)

- Code: `Main.handleUnoCall`, `GameState.hasOneCard`, `GameState.callUno`,
  `GameState.applyMissedUnoPenalty`, `ConsoleView.promptCallUno`
- Tests: `UnoCallPenaltyTest`

## Round End 

A round ends when a player empties their hand; that player wins the round.

- Code: `Main.handleWinIfAny`, `Main.runTurnLoop`
- Tests: `ScoringTest.aPlayedRoundCreditsExactlyOneWinnerWithOpponentPoints`

## Scoring 

The round winner scores the total point value of every card left in the other
players' hands: number cards score face value, Skip/Reverse/Draw Two score 20,
and Wild/Wild Draw Four score 50.

- Code: `Rules.scoreForWinner`, `Rules.handValue`, `Card.points`
- Tests: `ScoringTest`

## Multi-Round Game to Target Score 

A match is a series of rounds whose scores accumulate. By default the match
continues until a player reaches the target score (**500** by default, settable
with `--target N`); the highest-scoring player is the final winner.
`--games N` instead plays a fixed number of rounds. A safety cap of 1000 rounds
prevents an unbounded match.

- Code: `Main.playMatch`, `Main.highestScoringPlayer`
- Tests: `MatchTargetTest`

## Starting up-card 

If the first flipped up-card is a Wild, it is returned to the deck area and
another card is flipped, so a round never starts on a naked wild. Other action
cards used as the starting up-card do not apply their effect to the first player
(the effect only fires when a player actually plays the card).

- Code: `GameState.flipInitialUpCard`

## Summary of variants and simplifications

- Two-player Reverse acts as Skip.
- No Draw Two / Draw Four stacking.
- No Wild Draw Four challenge; Wild Draw Four is always playable.
- Draw/pass variant: draw one, play it if legal, otherwise pass.
- Missed-UNO penalty is checked immediately on reaching one card (two-card penalty).
- Starting wild up-card is re-flipped; other starting action cards do not fire.
- Fixed default target score of 500 (configurable via `--target`).
