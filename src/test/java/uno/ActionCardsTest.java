package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Skip, Reverse, and Draw Two effects. Effects are
 * exercised directly on a {@link GameState} with no console input, verifying
 * turn advancement and forced draws.
 */
public class ActionCardsTest {

    private ConsoleView view;

    @BeforeEach
    void quiet() {
        Main.quiet = true;
        view = new ConsoleView();
    }

    private GameState game(int players) {
        GameState s = new GameState();
        for (int i = 0; i < players; i++) {
            s.playerNames.add("P" + i);
            s.humanPlayers.add(Boolean.FALSE);
            s.hands.add(new ArrayList<Card>());
        }
        return s;
    }

    // ---------- Skip ----------

    @Test
    void skipMakesNextPlayerLoseTheirTurn() {
        GameState s = game(4);
        s.currentPlayer = 0;
        s.direction = 1;
        CardEffect.SKIP.apply(s, view);
        assertEquals(2, s.currentPlayer, "player 1 is skipped, turn lands on player 2");
    }

    @Test
    void skipWrapsAroundTheTable() {
        GameState s = game(3);
        s.currentPlayer = 2;
        s.direction = 1;
        CardEffect.SKIP.apply(s, view);
        assertEquals(1, s.currentPlayer, "skip player 0, land on player 1");
    }

    // ---------- Reverse ----------

    @Test
    void reverseFlipsDirectionForThreePlusPlayers() {
        GameState s = game(4);
        s.currentPlayer = 1;
        s.direction = 1;
        CardEffect.REVERSE.apply(s, view);
        assertEquals(-1, s.direction, "direction flips");
        assertEquals(0, s.currentPlayer, "turn moves the other way, 1 -> 0");
    }

    @Test
    void reverseActsLikeSkipInTwoPlayerGame() {
        GameState s = game(2);
        s.currentPlayer = 0;
        s.direction = 1;
        CardEffect.REVERSE.apply(s, view);
        assertEquals(0, s.currentPlayer, "in 2p Reverse behaves as Skip: same player continues");
    }

    // ---------- Draw Two ----------

    @Test
    void drawTwoMakesNextPlayerDrawTwoAndLoseTurn() {
        GameState s = game(3);
        s.deck.add(Card.of("R3"));
        s.deck.add(Card.of("Y4"));
        s.currentPlayer = 0;
        s.direction = 1;
        CardEffect.DRAW_TWO.apply(s, view);
        assertEquals(2, s.hands.get(1).size(), "victim (player 1) drew two cards");
        assertEquals("R3", s.hands.get(1).get(0).code());
        assertEquals("Y4", s.hands.get(1).get(1).code());
        assertEquals(2, s.currentPlayer, "victim is skipped, turn lands on player 2");
    }
}
