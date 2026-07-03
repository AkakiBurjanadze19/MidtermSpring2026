package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * round scoring. Number cards score face value,
 * action cards 20, and wilds 50; the round winner earns the total of every
 * card left in the other players' hands.
 */
public class ScoringTest {

    @BeforeEach
    void quiet() {
        Main.quiet = true;
    }

    private static List<Card> hand(String... codes) {
        ArrayList<Card> h = new ArrayList<>();
        for (String c : codes) {
            h.add(Card.of(c));
        }
        return h;
    }

    @Test
    void handValueSumsCardPoints() {
        assertEquals(75, Rules.handValue(hand("R5", "Y0", "BS", "W4")), "5 + 0 + 20 + 50");
        assertEquals(60, Rules.handValue(hand("R+2", "YR", "GS")), "20 + 20 + 20");
        assertEquals(0, Rules.handValue(hand()), "empty hand");
    }

    @Test
    void individualCardPointValues() {
        assertEquals(7, Card.of("R7").points());
        assertEquals(0, Card.of("G0").points());
        assertEquals(20, Card.of("BS").points());
        assertEquals(20, Card.of("YR").points());
        assertEquals(20, Card.of("R+2").points());
        assertEquals(50, Card.of("W").points());
        assertEquals(50, Card.of("W4").points());
    }

    @Test
    void winnerScoresSumOfOpponentHands() {
        GameState s = new GameState();
        for (int i = 0; i < 3; i++) {
            s.hands.add(new ArrayList<Card>());
        }
        // Player 0 wins with an empty hand.
        s.hands.set(0, new ArrayList<Card>());
        s.hands.set(1, (ArrayList<Card>) hand("R5", "BS"));   // 5 + 20 = 25
        s.hands.set(2, (ArrayList<Card>) hand("W", "G3"));    // 50 + 3 = 53

        assertEquals(78, Rules.scoreForWinner(s, 0), "25 + 53");
    }

    @Test
    void aPlayedRoundCreditsExactlyOneWinnerWithOpponentPoints() {
        Main.state = new GameState();
        Main.setupPlayers(3, false);
        Main.state.setSeed(42L);
        Main.playRound();

        int winner = -1, emptyHands = 0;
        for (int i = 0; i < Main.state.hands.size(); i++) {
            if (Main.state.hands.get(i).isEmpty()) {
                winner = i;
                emptyHands++;
            }
        }
        assertEquals(1, emptyHands, "exactly one player emptied their hand");

        int expected = 0;
        for (int i = 0; i < Main.state.hands.size(); i++) {
            if (i != winner) {
                expected += Rules.handValue(Main.state.hands.get(i));
            }
        }
        assertEquals(expected, Main.state.scores[winner], "winner scored opponents' card points");
        assertTrue(Main.state.scores[winner] > 0);
    }
}
