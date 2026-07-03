package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies one-card detection,
 * recording a call, the two-card penalty for a missed call, and that a bot
 * (which always remembers) reaches one card safely.
 */
public class UnoCallPenaltyTest {

    @BeforeEach
    void quiet() {
        Main.quiet = true;
    }

    @Test
    void oneCardStateIsDetected() {
        GameState s = new GameState();
        s.hands.add(new ArrayList<Card>());
        s.hands.get(0).add(Card.of("R5"));
        assertTrue(s.hasOneCard(0));
        s.hands.get(0).add(Card.of("B2"));
        assertFalse(s.hasOneCard(0), "two cards is not the UNO state");
    }

    @Test
    void callingUnoIsRecorded() {
        GameState s = new GameState();
        assertFalse(s.saidUno[0]);
        s.callUno(0);
        assertTrue(s.saidUno[0]);
    }

    @Test
    void missedUnoPenaltyDrawsTwoCards() {
        GameState s = new GameState();
        s.hands.add(new ArrayList<Card>());
        s.hands.get(0).add(Card.of("R5"));       // player is at one card
        s.deck.add(Card.of("G1"));
        s.deck.add(Card.of("G2"));

        int drawn = s.applyMissedUnoPenalty(0);

        assertEquals(2, drawn, "penalty is two cards");
        assertEquals(3, s.hands.get(0).size(), "one card plus two penalty cards");
        assertEquals(GameState.MISSED_UNO_PENALTY, drawn);
    }

    @Test
    void botReachingOneCardCallsUnoAndAvoidsPenalty() {
        Main.state = new GameState();
        Main.setupPlayers(2, false);   // Bot1, Bot2
        Main.state.currentPlayer = 0;
        Main.state.hands.get(0).add(Card.of("R5"));   // Bot1 down to one card

        Main.handleUnoCall("Bot1");

        assertTrue(Main.state.saidUno[0], "bot called UNO");
        assertEquals(1, Main.state.hands.get(0).size(), "no penalty applied");
    }

    @Test
    void aFreshRoundClearsStaleUnoFlags() {
        // Pre-set every flag, then start a round. playRound installs a fresh
        // all-false array, so the only true flags afterward belong to players
        // who genuinely called UNO during this round (not the stale pre-sets).
        Main.state = new GameState();
        Main.setupPlayers(3, false);
        for (int i = 0; i < 3; i++) {
            Main.state.saidUno[i] = true;
        }

        Main.state.setSeed(7L);
        Main.playRound();

        // At least one loser never reached one card this round, so the stale
        // "true" it started with must have been cleared.
        boolean someFlagCleared = false;
        for (int i = 0; i < Main.state.playerNames.size(); i++) {
            if (!Main.state.saidUno[i]) {
                someFlagCleared = true;
            }
        }
        assertTrue(someFlagCleared, "stale pre-round UNO flags were reset");
    }
}
