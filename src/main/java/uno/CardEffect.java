package uno;
/**
 * Strategy for what happens after a card has been played: turn advancement
 * and any forced draws on subsequent players. The play itself, discard-pile
 * update, called-color choice, "UNO" announcement, and win check are
 * universal and stay in the turn loop; this interface lets the per-card
 * consequence be selected polymorphically instead of via a chain of
 * {@code if (rank(card).equals("X"))} branches.
 * 
 */
public interface CardEffect {
    void apply(GameState state, ConsoleView view);

    // Singletons. Effects are stateless, so we share one instance per rank.
    CardEffect SKIP = new SkipEffect();
    CardEffect REVERSE = new ReverseEffect();
    CardEffect DRAW_TWO = new DrawTwoEffect();
    CardEffect WILD_DRAW_FOUR = new WildDrawFourEffect();
    CardEffect NORMAL = new NormalEffect();
    
    static CardEffect forRank(String rank) {
        if (rank.equals("SKIP")) return SKIP;
        if (rank.equals("REVERSE")) return REVERSE;
        if (rank.equals("DRAW_TWO")) return DRAW_TWO;
        if (rank.equals("WILD_DRAW_FOUR")) return WILD_DRAW_FOUR;
        return NORMAL;
    }
}

/** Skip the next player by calling {@code next()} twice. */
class SkipEffect implements CardEffect {
    public void apply(GameState state, ConsoleView view) {
        state.next();
        state.next();
    }
}

/**
 * Flip the direction of play. With two players, this behaves like a SKIP
 * (two {@code next()} calls in the new direction bring the turn back to the
 * same player), matching the documented quirk of the original implementation.
 */
class ReverseEffect implements CardEffect {
    public void apply(GameState state, ConsoleView view) {
        state.direction = state.direction * -1;
        if (state.playerNames.size() == 2) {
            state.next();
            state.next();
        } else {
            state.next();
        }
    }
}

/** Force the next player to draw two cards, then skip them. */
class DrawTwoEffect implements CardEffect {
    public void apply(GameState state, ConsoleView view) {
        state.next();
        state.hands.get(state.currentPlayer).add(state.draw());
        state.hands.get(state.currentPlayer).add(state.draw());
        view.announceDrawTwo(state.playerNames.get(state.currentPlayer));
        state.next();
    }
}

/** Force the next player to draw four cards, then skip them. */
class WildDrawFourEffect implements CardEffect {
    public void apply(GameState state, ConsoleView view) {
        state.next();
        for (int i = 0; i < 4; i++) {
            state.hands.get(state.currentPlayer).add(state.draw());
        }
        view.announceDrawFour(state.playerNames.get(state.currentPlayer));
        state.next();
    }
}

/** Plain number/wild card with no extra effect: just advance the turn. */
class NormalEffect implements CardEffect {
    public void apply(GameState state, ConsoleView view) {
        state.next();
    }
}
