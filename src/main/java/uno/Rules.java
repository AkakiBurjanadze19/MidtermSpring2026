package uno;

import java.util.List;

/**
 * The rulebook. This class is the single, console-free home for UNO rule
 * decisions that do not mutate game state:
 *
 * <ul>
 *   <li>{@link #isLegal(Card, PlayContext)} – whether a card may be played on
 *       the current up-card / called color.</li>
 *   <li>{@link #scoreForWinner(GameState, int)} – how many points a round
 *       winner earns from the cards left in the losers' hands.</li>
 * </ul>
 *
 * Everything here is a pure function of its arguments, so the rules can be
 * exercised directly from tests without a deck, a console, or a running game
 * loop. {@link CardEffect} is the companion home for rules that <em>do</em>
 * mutate state (turn advancement and forced draws).
 */
public final class Rules {

    private Rules() {
    }

    /**
     * A card is legal to play when at least one of these holds:
     * <ul>
     *   <li>it is a Wild or Wild Draw Four (always playable);</li>
     *   <li>its color matches the active color (the up-card's color, or the
     *       color called by a wild);</li>
     *   <li>it is an action card whose action matches the up-card's action
     *       (Skip on Skip, Reverse on Reverse, Draw Two on Draw Two);</li>
     *   <li>it is a number card whose number matches the up-card's number.</li>
     * </ul>
     */
    public static boolean isLegal(Card card, PlayContext context) {
        if (card.isWild()) {
            return true;
        }
        CardColor active = activeColor(context);
        if (card.color() == active) {
            return true;
        }
        if (card.rank().equals(context.upCard().rank()) && !card.rank().equals("NUMBER")) {
            return true;
        }
        if (card.rank().equals("NUMBER") && context.upCard().rank().equals("NUMBER")
                && card.number() == context.upCard().number()) {
            return true;
        }
        return false;
    }

    /**
     * The color a play must currently match: the color called by the most
     * recent wild if one is in effect, otherwise the up-card's own color.
     */
    public static CardColor activeColor(PlayContext context) {
        if (context.calledColor() != CardColor.NONE) {
            return context.calledColor();
        }
        return context.upCard().color();
    }

    /**
     * Points the winner of a round earns: the sum of the point values of every
     * card still held by the other players. Number cards score their face
     * value, action cards score 20, and wilds score 50 (see {@link Card#points()}).
     */
    public static int scoreForWinner(GameState state, int winnerIndex) {
        int points = 0;
        for (int i = 0; i < state.hands.size(); i++) {
            if (i != winnerIndex) {
                points += handValue(state.hands.get(i));
            }
        }
        return points;
    }

    /** Sum of the point values of the cards in a single hand. */
    public static int handValue(List<Card> hand) {
        int points = 0;
        for (int i = 0; i < hand.size(); i++) {
            points += hand.get(i).points();
        }
        return points;
    }
}
