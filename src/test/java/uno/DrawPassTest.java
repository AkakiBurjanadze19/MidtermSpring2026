package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The chosen variant: a player with no legal
 * play draws exactly one card; if it is legal they play it immediately (bots
 * always do), otherwise the turn passes with the drawn card kept in hand.
 */
public class DrawPassTest {

    @BeforeEach
    void freshQuietGame() {
        Main.quiet = true;
        Main.state = new GameState();
        Main.setupPlayers(2, false);   // Bot1, Bot2 (both computer players)
        Main.state.currentPlayer = 0;
    }

    @Test
    void playerWithALegalCardDoesNotDraw() {
        Main.state.upCard = Card.of("R9");
        ArrayList<Card> hand = Main.state.hands.get(0);
        hand.add(Card.of("R2"));
        hand.add(Card.of("Y3"));
        Main.state.deck.add(Card.of("G4"));

        int chosen = Main.pickCardOrDraw(hand, "Bot1");

        assertEquals(0, chosen, "plays the legal R2");
        assertEquals(1, Main.state.deck.size(), "deck untouched");
        assertEquals(2, hand.size(), "hand unchanged");
    }

    @Test
    void playerDrawsAndPlaysWhenDrawnCardIsLegal() {
        Main.state.upCard = Card.of("R9");
        ArrayList<Card> hand = Main.state.hands.get(0);
        hand.add(Card.of("Y3"));           // not legal on R9
        Main.state.deck.add(Card.of("R4")); // legal once drawn

        int chosen = Main.pickCardOrDraw(hand, "Bot1");

        assertEquals(1, chosen, "returns the index of the freshly drawn legal card");
        assertEquals("R4", hand.get(1).code());
    }

    @Test
    void playerPassesWhenDrawnCardIsNotLegal() {
        Main.state.upCard = Card.of("R9");
        ArrayList<Card> hand = Main.state.hands.get(0);
        hand.add(Card.of("Y3"));           // not legal
        Main.state.deck.add(Card.of("G4")); // still not legal on R9

        int chosen = Main.pickCardOrDraw(hand, "Bot1");

        assertEquals(-1, chosen, "passes the turn");
        assertEquals(2, hand.size(), "drawn card is kept in hand");
        assertEquals("G4", hand.get(1).code());
    }
}
