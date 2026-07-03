package uno;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the standard 108-card UNO
 * deck: four colors, the 0/1-9 number spread, two of each action card per
 * color, and four of each wild.
 */
public class DeckCompositionTest {

    private Map<String, Integer> counts() {
        GameState state = new GameState();
        state.buildStandardDeck();
        Map<String, Integer> counts = new HashMap<>();
        for (Card c : state.deck) {
            counts.merge(c.code(), 1, Integer::sum);
        }
        return counts;
    }

    @Test
    void deckHasExactly108Cards() {
        GameState state = new GameState();
        state.buildStandardDeck();
        assertEquals(108, state.deck.size());
    }

    @Test
    void oneZeroAndTwoOfEachNumberPerColor() {
        Map<String, Integer> counts = counts();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            assertEquals(1, counts.get(color + "0"), color + "0 should appear once");
            for (int n = 1; n <= 9; n++) {
                assertEquals(2, counts.get(color + n), color + n + " should appear twice");
            }
        }
    }

    @Test
    void twoOfEachActionCardPerColor() {
        Map<String, Integer> counts = counts();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            assertEquals(2, counts.get(color + "S"), "two Skips per color");
            assertEquals(2, counts.get(color + "R"), "two Reverses per color");
            assertEquals(2, counts.get(color + "+2"), "two Draw Twos per color");
        }
    }

    @Test
    void fourWildsAndFourWildDrawFours() {
        Map<String, Integer> counts = counts();
        assertEquals(4, counts.get("W"), "four Wild cards");
        assertEquals(4, counts.get("W4"), "four Wild Draw Four cards");
    }

    @Test
    void allFourColorsPresent() {
        Map<String, Integer> counts = counts();
        for (String color : new String[]{"R", "Y", "G", "B"}) {
            assertTrue(counts.keySet().stream().anyMatch(code -> code.startsWith(color)),
                    "color " + color + " should be present");
        }
    }
}
