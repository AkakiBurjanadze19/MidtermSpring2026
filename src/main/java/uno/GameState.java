package uno;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Encapsulated game state for the UNO CLI.
 *
 * All mutable game data lives here: deck and discard piles (both
 * {@code ArrayList<Card>}), each player's hand, whose turn it is, play
 * direction, the current up-card and called color, cumulative scores, and the
 * seeded random source. Pure state operations (draw, next, deck construction)
 * live here too so rule code in {@link Main} never needs to import
 * {@link Collections} or {@link Random}.
 *
 * Fields are package-private to allow {@link CharacterizationTests} to set up
 * scenarios directly without breaking the class boundary.
 */
public class GameState {

    final ArrayList<String> playerNames = new ArrayList<String>();
    final ArrayList<Boolean> humanPlayers = new ArrayList<Boolean>();
    final ArrayList<ArrayList<Card>> hands = new ArrayList<ArrayList<Card>>();
    final ArrayList<Card> deck = new ArrayList<Card>();
    final ArrayList<Card> discard = new ArrayList<Card>();
    int[] scores = new int[10];
    int currentPlayer = 0;
    int direction = 1;
    Card upCard = Card.of("R0");
    CardColor calledColor = CardColor.NONE;
    Random random = new Random();

    /** Reseed the RNG so games are reproducible with {@code --seed N}. */
    void setSeed(long seed) {
        random.setSeed(seed);
    }

    /** Advance {@link #currentPlayer} by {@link #direction}, wrapping around. */
    void next() {
        currentPlayer += direction;
        if (currentPlayer >= playerNames.size()) {
            currentPlayer = 0;
        }
        if (currentPlayer < 0) {
            currentPlayer = playerNames.size() - 1;
        }
    }

    /**
     * Draw the top card. If the deck is empty, the discard pile is shuffled
     * back in. If both are empty, returns the {@code "W"} sentinel card.
     */
    Card draw() {
        if (deck.size() == 0) {
            deck.addAll(discard);
            discard.clear();
            Collections.shuffle(deck, random);
        }
        if (deck.size() == 0) {
            return Card.of("W");
        }
        return deck.remove(0);
    }

    /** Shuffle the deck in place using {@link #random}. */
    void shuffleDeck() {
        Collections.shuffle(deck, random);
    }

    /**
     * Clear all player hands and deal {@code cardsEach} fresh cards to each
     * player from the deck.
     */
    void dealStartingHands(int cardsEach) {
        for (int i = 0; i < hands.size(); i++) {
            hands.get(i).clear();
        }
        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = 0; j < cardsEach; j++) {
                hands.get(i).add(draw());
            }
        }
    }

    /**
     * Flip the first up-card. If it is a wild it is pushed to the discard
     * and another card is drawn, preserving the documented quirk that a round
     * can never start with a naked wild as the up-card.
     */
    void flipInitialUpCard() {
        upCard = draw();
        while (upCard.isWild()) {
            discard.add(upCard);
            upCard = draw();
        }
    }

    /**
     * Fill the deck with a standard 108-card UNO deck: one 0, two each of
     * 1..9, two SKIPs, two REVERSEs and two DRAW_TWOs per color, plus four
     * WILDs and four WILD_DRAW_FOURs.
     */
    void buildStandardDeck() {
        deck.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            deck.add(Card.of(colors[c] + "0"));
            for (int n = 1; n <= 9; n++) {
                deck.add(Card.of(colors[c] + n));
                deck.add(Card.of(colors[c] + n));
            }
            deck.add(Card.of(colors[c] + "S"));
            deck.add(Card.of(colors[c] + "S"));
            deck.add(Card.of(colors[c] + "R"));
            deck.add(Card.of(colors[c] + "R"));
            deck.add(Card.of(colors[c] + "+2"));
            deck.add(Card.of(colors[c] + "+2"));
        }
        for (int i = 0; i < 4; i++) {
            deck.add(Card.of("W"));
            deck.add(Card.of("W4"));
        }
    }
}
