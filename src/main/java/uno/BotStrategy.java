package uno;

import java.util.List;

/**
 * Decision logic for computer players, kept in one console-free home so bot
 * behavior can be unit-tested directly.
 *
 * The strategy is intentionally simple but plays every legal action card:
 * given a hand and the current play target it returns the index of the card to
 * play, or {@code -1} to signal "no legal play, draw instead". Card choice
 * follows a fixed priority so games are deterministic under a fixed seed:
 *
 * <pre>Draw Two &gt; Skip &gt; Reverse &gt; Number &gt; Wild</pre>
 *
 * Aggressive action cards are played first, plain numbers next, and wilds are
 * held back as a last resort so their color-changing flexibility is not wasted.
 */
public final class BotStrategy {

    private BotStrategy() {
    }

    /** The order in which the bot prefers to play colored/action cards. */
    private static final String[] PRIORITY = {"DRAW_TWO", "SKIP", "REVERSE", "NUMBER"};

    /**
     * Choose which card to play from {@code hand} given the current play
     * target, or {@code -1} to draw. Colored cards are considered in priority
     * order; a wild is chosen only when nothing colored is legal.
     */
    public static int chooseCard(List<Card> hand, PlayContext context) {
        for (int p = 0; p < PRIORITY.length; p++) {
            for (int i = 0; i < hand.size(); i++) {
                Card card = hand.get(i);
                if (card.rank().equals(PRIORITY[p]) && Rules.isLegal(card, context)) {
                    return i;
                }
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).isWild()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * After playing a wild, choose the color to call. Picks the color the bot
     * holds the most of, breaking ties in the fixed order R &gt; Y &gt; G &gt; B
     * so the choice is deterministic.
     */
    public static CardColor chooseColor(List<Card> hand) {
        int r = 0, y = 0, g = 0, b = 0;
        for (int i = 0; i < hand.size(); i++) {
            CardColor c = hand.get(i).color();
            if (c == CardColor.R) r++;
            else if (c == CardColor.Y) y++;
            else if (c == CardColor.G) g++;
            else if (c == CardColor.B) b++;
        }
        if (r >= y && r >= g && r >= b) return CardColor.R;
        if (y >= r && y >= g && y >= b) return CardColor.Y;
        if (g >= r && g >= y && g >= b) return CardColor.G;
        return CardColor.B;
    }

    /**
     * Whether the bot remembers to call UNO when it reaches one card. The bot
     * is a reliable player: it always calls, so it is never caught by the
     * missed-UNO penalty. (Human players must remember to call it themselves.)
     */
    public static boolean shouldCallUno() {
        return true;
    }
}
