package uno;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bot decision logic. Confirms
 * the play priority Draw Two &gt; Skip &gt; Reverse &gt; Number &gt; Wild, that
 * the bot now plays a legal Reverse (previously a bug), and the draw fallback.
 */
public class BotStrategyTest {

    private static List<Card> hand(String... codes) {
        ArrayList<Card> h = new ArrayList<>();
        for (String c : codes) {
            h.add(Card.of(c));
        }
        return h;
    }

    private static PlayContext on(String up) {
        return new PlayContext(Card.of(up), CardColor.NONE);
    }

    @Test
    void prefersDrawTwoOverEverythingLegal() {
        assertEquals(2, BotStrategy.chooseCard(hand("R3", "RS", "R+2", "W"), on("R9")));
    }

    @Test
    void prefersSkipOverReverseNumberWild() {
        assertEquals(1, BotStrategy.chooseCard(hand("R3", "RS", "RR", "W"), on("R9")));
    }

    @Test
    void playsALegalReverseInsteadOfDrawing() {
        // Regression: bots used to ignore Reverse and draw instead.
        assertEquals(0, BotStrategy.chooseCard(hand("RR", "Y3"), on("R9")));
    }

    @Test
    void prefersNumberOverWild() {
        assertEquals(0, BotStrategy.chooseCard(hand("R3", "W"), on("R9")));
    }

    @Test
    void fallsBackToWildWhenNothingColoredIsLegal() {
        assertEquals(2, BotStrategy.chooseCard(hand("Y3", "B7", "W4"), on("R9")));
    }

    @Test
    void drawsWhenNoLegalPlayExists() {
        assertEquals(-1, BotStrategy.chooseCard(hand("Y3", "B7"), on("R9")));
    }

    @Test
    void colorChoiceBreaksTiesRedYellowGreenBlue() {
        assertEquals(CardColor.R, BotStrategy.chooseColor(hand("R1", "Y1", "G1", "B1")));
        assertEquals(CardColor.Y, BotStrategy.chooseColor(hand("Y1", "G1", "B1")));
        assertEquals(CardColor.G, BotStrategy.chooseColor(hand("G1", "B1")));
    }
}
