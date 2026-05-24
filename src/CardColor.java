/**
 * The four card colors in UNO, plus {@link #NONE} for wilds and for
 * {@link GameState#calledColor} when no wild is currently in effect.
 */
public enum CardColor {
    R, Y, G, B,
    /** No color: returned by wild cards and the initial/reset value of calledColor. */
    NONE;
}
