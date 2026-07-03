package uno;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *  Exercises every branch of
 * {@link Rules#isLegal}: match by color, number, and action type; wilds always
 * playable; the called color after a wild; and rejection of illegal plays.
 */
public class LegalPlayTest {

    private static PlayContext on(String upCard) {
        return new PlayContext(Card.of(upCard), CardColor.NONE);
    }

    private static PlayContext on(String upCard, CardColor called) {
        return new PlayContext(Card.of(upCard), called);
    }

    private static boolean legal(String card, PlayContext ctx) {
        return Rules.isLegal(Card.of(card), ctx);
    }

    @Test
    void matchesByColor() {
        assertTrue(legal("R2", on("R9")));
        assertTrue(legal("B0", on("B7")));
    }

    @Test
    void matchesByNumber() {
        assertTrue(legal("G9", on("R9")), "same number, different color");
        assertTrue(legal("Y0", on("B0")));
    }

    @Test
    void matchesByActionType() {
        assertTrue(legal("RS", on("YS")), "Skip on Skip across colors");
        assertTrue(legal("BR", on("GR")), "Reverse on Reverse across colors");
        assertTrue(legal("R+2", on("B+2")), "Draw Two on Draw Two across colors");
    }

    @Test
    void wildsAreAlwaysLegal() {
        assertTrue(legal("W", on("R5")));
        assertTrue(legal("W4", on("YS")));
        assertTrue(legal("W", on("W")));
    }

    @Test
    void calledColorAfterWildControlsLegality() {
        assertTrue(legal("B3", on("W", CardColor.B)), "matches called color");
        assertFalse(legal("R3", on("W", CardColor.B)), "does not match called color");
    }

    @Test
    void calledColorOverridesUpCardColor() {
        // Up card is a red 5, but a wild called blue: only blue (or wild) is legal.
        PlayContext ctx = on("R5", CardColor.B);
        assertTrue(legal("B1", ctx));
        assertFalse(legal("R1", ctx), "red no longer legal once blue is called");
    }

    @Test
    void illegalPlaysAreRejected() {
        assertFalse(legal("R5", on("Y3")), "different color and number");
        assertFalse(legal("RS", on("Y3")), "action vs number");
        assertFalse(legal("RS", on("YR")), "Skip vs Reverse are different actions");
    }
}
