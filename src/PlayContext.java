/**
 * Immutable value object describing the current play target: the card on top
 * of the discard pile and any color called by a wild that is still in effect
 * ({@link CardColor#NONE} when no called color is active).
 *
 * The legal-play rule in {@link Main#isLegal} operates on this pair. Bundling
 * them here eliminates the duplicate {@code (upCard, calledColor)} argument
 * pair that previously appeared at seven call sites.
 */
public final class PlayContext {

    private final Card upCard;
    private final CardColor calledColor;

    public PlayContext(Card upCard, CardColor calledColor) {
        this.upCard = upCard;
        this.calledColor = calledColor;
    }

    /** Snapshot the current play target from a live {@link GameState}. */
    public static PlayContext of(GameState state) {
        return new PlayContext(state.upCard, state.calledColor);
    }

    public Card upCard() {
        return upCard;
    }

    public CardColor calledColor() {
        return calledColor;
    }

    @Override
    public String toString() {
        return calledColor == CardColor.NONE
                ? upCard.code()
                : upCard.code() + " called " + calledColor.name();
    }
}
