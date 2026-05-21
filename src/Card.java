/**
 * Immutable value object for an UNO card.
 *
 * Cards are still represented internally by the same compact string codes used
 * throughout the rest of the codebase:
 *
 *   R0..R9, Y0..Y9, G0..G9, B0..B9   number cards
 *   RS, YS, GS, BS                   skip
 *   RR, YR, GR, BR                   reverse
 *   R+2, Y+2, G+2, B+2               draw two
 *   W                                wild
 *   W4                               wild draw four
 *
 * The intent of this class is to give "card behavior" a clear home: parsing
 * a card's color/rank/numeric value/scoring lives here rather than scattered
 * across {@link Main}. The static helpers on {@code Main} remain as thin
 * package-private adapters so that existing callers (the turn loop, the bot,
 * the characterization tests) continue to work without churn.
 *
 * This class is final and stateless; all methods are pure functions of the
 * underlying code, so {@code Card} instances are safe to share and compare
 * by value.
 */
public final class Card {

    private final String code;

    private Card(String code) {
        this.code = code;
    }

    /** Factory. Wraps a card string code (e.g. "R5", "YS", "W4"). */
    public static Card of(String code) {
        return new Card(code);
    }

    /** The raw string code, e.g. "R5". */
    public String code() {
        return code;
    }

    /** True for both "W" (wild) and "W4" (wild draw four). */
    public boolean isWild() {
        return code.startsWith("W");
    }

    /** The card's color, or {@link CardColor#NONE} for wilds. */
    public CardColor color() {
        if (code.startsWith("R")) return CardColor.R;
        if (code.startsWith("Y")) return CardColor.Y;
        if (code.startsWith("G")) return CardColor.G;
        if (code.startsWith("B")) return CardColor.B;
        return CardColor.NONE;
    }

    /**
     * One of: "NUMBER", "SKIP", "REVERSE", "DRAW_TWO", "WILD", "WILD_DRAW_FOUR".
     *
     * The order of checks matches the existing behavior in {@link Main#rank}:
     * exact-match wilds first, then suffix-based action ranks, with "NUMBER"
     * as the default fallback.
     */
    public String rank() {
        if (code.equals("W")) return "WILD";
        if (code.equals("W4")) return "WILD_DRAW_FOUR";
        if (code.endsWith("S")) return "SKIP";
        if (code.endsWith("R")) return "REVERSE";
        if (code.endsWith("+2")) return "DRAW_TWO";
        return "NUMBER";
    }

    /** Face value 0..9 for number cards; -1 sentinel for everything else. */
    public int number() {
        if (rank().equals("NUMBER")) {
            return Integer.parseInt(code.substring(1));
        }
        return -1;
    }

    /** Scoring used by the winner-tally at end of game. */
    public int points() {
        String r = rank();
        if (r.equals("NUMBER")) return number();
        if (r.equals("SKIP") || r.equals("REVERSE") || r.equals("DRAW_TWO")) return 20;
        if (r.equals("WILD") || r.equals("WILD_DRAW_FOUR")) return 50;
        return 0;
    }

    /**
     * The behavioral effect this card has when played: turn advancement and
     * any forced draws on the next player. See {@link CardEffect}.
     */
    public CardEffect effect() {
        return CardEffect.forRank(rank());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card)) return false;
        return code.equals(((Card) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code;
    }
}
