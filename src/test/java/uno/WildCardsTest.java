package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Wild and Wild Draw Four. Verifies that playing a wild lets
 * the player choose the next active color and that the choice constrains
 * subsequent legal plays, and that Wild Draw Four forces four cards on the next
 * player and skips them.
 */
public class WildCardsTest {

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

    @Test
    void botChoosesItsMajorityColorAfterWild() {
        ArrayList<Card> hand = new ArrayList<>();
        hand.add(Card.of("B1"));
        hand.add(Card.of("B2"));
        hand.add(Card.of("R3"));
        assertEquals(CardColor.B, BotStrategy.chooseColor(hand));
    }

    @Test
    void chosenColorBecomesActiveAndAffectsLegality() {
        // A bot plays a Wild; the called color must then drive legal-play checks.
        Main.quiet = true;
        Main.state = new GameState();
        Main.setupPlayers(2, false);       // Bot1, Bot2
        Main.state.currentPlayer = 0;
        ArrayList<Card> hand = Main.state.hands.get(0);
        hand.add(Card.of("W"));
        hand.add(Card.of("G5"));           // green majority -> bot will call GREEN

        Main.applyPlay(0, hand, "Bot1");

        assertEquals(CardColor.G, Main.state.calledColor, "bot called its majority color");
        PlayContext ctx = PlayContext.of(Main.state);
        assertTrue(Rules.isLegal(Card.of("G1"), ctx), "green now legal");
        assertFalse(Rules.isLegal(Card.of("R1"), ctx), "red now illegal");
    }

    @Test
    void wildDrawFourMakesNextPlayerDrawFourAndLoseTurn() {
        GameState s = game(3);
        for (String code : new String[]{"R1", "R2", "R3", "R4"}) {
            s.deck.add(Card.of(code));
        }
        s.currentPlayer = 0;
        s.direction = 1;
        CardEffect.WILD_DRAW_FOUR.apply(s, view);
        assertEquals(4, s.hands.get(1).size(), "victim drew four cards");
        assertEquals(2, s.currentPlayer, "victim is skipped, turn lands on player 2");
    }
}
