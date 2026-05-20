import java.util.ArrayList;

/**
 * Controller / entry point for the UNO CLI.
 *
 * Holds the orchestration logic (CLI argument parsing, turn loop, bot
 * strategy, and legal-play rule) but does not own game state or console I/O.
 * Those live in {@link GameState} and {@link ConsoleView}; card behavior and
 * card-color semantics live on {@link Card} and {@link CardColor}.
 */
public class Main {

    static GameState state = new GameState();
    static boolean quiet = false;
    static ConsoleView view = new ConsoleView();

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        long seed = System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--bots") && i + 1 < args.length) {
                bots = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--games") && i + 1 < args.length) {
                games = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--human")) {
                human = true;
            } else if (args[i].equals("--quiet")) {
                quiet = true;
            } else if (args[i].equals("--seed") && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else if (args[i].equals("--self-test")) {
                selfTest();
                return;
            } else if (args[i].equals("--help")) {
                view.showHelp();
                return;
            }
        }

        state.setSeed(seed);
        setupPlayers(bots, human);

        if (state.playerNames.size() < 2 || state.playerNames.size() > 4) {
            view.showError("UNO needs 2 to 4 players.");
            return;
        }

        for (int g = 1; g <= games; g++) {
            view.announceGame(g);
            playGame();
        }

        view.showFinalScores(state.playerNames, state.scores);
    }

    static void setupPlayers(int bots, boolean human) {
        state.playerNames.clear();
        state.humanPlayers.clear();
        state.hands.clear();
        if (human) {
            state.playerNames.add("You");
            state.humanPlayers.add(Boolean.TRUE);
            state.hands.add(new ArrayList<Card>());
        }
        for (int i = 1; i <= bots; i++) {
            state.playerNames.add("Bot" + i);
            state.humanPlayers.add(Boolean.FALSE);
            state.hands.add(new ArrayList<Card>());
        }
    }

    static void playGame() {
        startNewRound();
        runTurnLoop();
    }

    static void startNewRound() {
        state.buildStandardDeck();
        state.shuffleDeck();
        state.discard.clear();
        state.dealStartingHands(7);
        state.flipInitialUpCard();
        state.calledColor = CardColor.NONE;
        state.direction = 1;
        state.currentPlayer = state.random.nextInt(state.playerNames.size());
    }

    static void runTurnLoop() {
        int guard = 0;
        while (guard < 3000) {
            guard++;
            if (playSingleTurn()) {
                return;
            }
        }
        view.announceSafetyLimit();
    }

    /** One turn for the current player. Returns true if the game has ended. */
    static boolean playSingleTurn() {
        String name = state.playerNames.get(state.currentPlayer);
        ArrayList<Card> hand = state.hands.get(state.currentPlayer);

        view.showUpCard(state.upCard, state.calledColor);
        view.showHand(name, hand);

        int chosen = pickCardOrDraw(hand, name);
        if (chosen < 0) {
            state.next();
            return false;
        }

        if (!isValidPlay(chosen, hand, name)) {
            hand.add(state.draw());
            state.next();
            return false;
        }

        Card card = applyPlay(chosen, hand, name);

        if (handleWinIfAny(name)) {
            return true;
        }

        card.effect().apply(state, view);
        return false;
    }

    /**
     * Ask the current player for a card index, or draw one and decide whether
     * to auto-play it. Returns the index to play, or -1 to pass the turn.
     */
    static int pickCardOrDraw(ArrayList<Card> hand, String name) {
        int chosen = isCurrentHuman() ? askHuman(hand) : chooseBotCard(hand);
        if (chosen != -1) {
            return chosen;
        }

        Card drawn = state.draw();
        hand.add(drawn);
        view.announceDraw(name, drawn);

        if (!isLegal(drawn, PlayContext.of(state))) {
            return -1;
        }
        if (!isCurrentHuman()) {
            return hand.size() - 1;
        }
        return view.promptPlayDrawnCard(drawn) ? hand.size() - 1 : -1;
    }

    /**
     * Validate that {@code chosen} points at a legal card. Announces the
     * appropriate penalty message on failure.
     */
    static boolean isValidPlay(int chosen, ArrayList<Card> hand, String name) {
        if (chosen >= hand.size()) {
            view.announceIllegalIndex(name);
            return false;
        }
        Card card = hand.get(chosen);
        if (!isLegal(card, PlayContext.of(state))) {
            view.announceIllegalPlay(name, card);
            return false;
        }
        return true;
    }

    /**
     * Apply a confirmed-legal play: remove the card, update discard and
     * up-card, ask for a called color on wilds, announce the play and UNO.
     * Returns the played card so the caller can dispatch its effect.
     */
    static Card applyPlay(int chosen, ArrayList<Card> hand, String name) {
        Card card = hand.get(chosen);
        hand.remove(chosen);
        state.discard.add(state.upCard);
        state.upCard = card;
        state.calledColor = CardColor.NONE;
        view.announcePlay(name, card);

        if (card.isWild()) {
            state.calledColor = isCurrentHuman() ? askColor() : chooseBotColor(hand);
            view.announceColorCall(name, state.calledColor);
        }

        if (hand.size() == 1) {
            view.announceUno(name);
        }
        return card;
    }

    /**
     * If the current player has emptied their hand, credit them with opponents'
     * point total, announce the win, and return true.
     */
    static boolean handleWinIfAny(String name) {
        if (state.hands.get(state.currentPlayer).size() != 0) {
            return false;
        }
        int points = sumOpponentPoints();
        state.scores[state.currentPlayer] += points;
        view.announceWin(name, points);
        return true;
    }

    static int sumOpponentPoints() {
        int points = 0;
        for (int i = 0; i < state.hands.size(); i++) {
            if (i != state.currentPlayer) {
                for (int j = 0; j < state.hands.get(i).size(); j++) {
                    points += state.hands.get(i).get(j).points();
                }
            }
        }
        return points;
    }

    static boolean isCurrentHuman() {
        return state.humanPlayers.get(state.currentPlayer).booleanValue();
    }

    static int chooseBotCard(ArrayList<Card> hand) {
        PlayContext context = PlayContext.of(state);
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.rank().equals("DRAW_TWO") && isLegal(card, context)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.rank().equals("SKIP") && isLegal(card, context)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.rank().equals("NUMBER") && isLegal(card, context)) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).isWild()) {
                return i;
            }
        }
        return -1;
    }

    static int askHuman(ArrayList<Card> hand) {
        while (true) {
            String input = view.promptCardChoice();
            if (input.equals("DRAW")) {
                return -1;
            }
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
            } catch (Exception ignored) {
            }
            PlayContext context = PlayContext.of(state);
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).code().equals(input)) {
                    if (isLegal(hand.get(i), context)) {
                        return i;
                    }
                    view.notify("That card is not legal.");
                }
            }
            view.notify("Card not found.");
        }
    }

    static CardColor askColor() {
        while (true) {
            String input = view.promptColor();
            for (CardColor c : CardColor.values()) {
                if (c != CardColor.NONE && c.name().equals(input)) {
                    return c;
                }
            }
            view.notify("Bad color.");
        }
    }

    static CardColor chooseBotColor(ArrayList<Card> hand) {
        int r = 0, y = 0, g = 0, b = 0;
        for (int i = 0; i < hand.size(); i++) {
            CardColor c = hand.get(i).color();
            if (c == CardColor.R) r++;
            else if (c == CardColor.Y) y++;
            else if (c == CardColor.G) g++;
            else if (c == CardColor.B) b++;
        }
        if (r >= y && r >= g && r >= b) return CardColor.R;
        if (y >= r && y >= g && y >= b) return CardColor.Y;
        if (g >= r && g >= y && g >= b) return CardColor.G;
        return CardColor.B;
    }

    static boolean isLegal(Card card, PlayContext context) {
        if (card.isWild()) return true;
        if (card.color() == context.upCard().color()) return true;
        if (context.calledColor() != CardColor.NONE && card.color() == context.calledColor()) return true;
        if (card.rank().equals(context.upCard().rank()) && !card.rank().equals("NUMBER")) return true;
        if (card.rank().equals("NUMBER") && context.upCard().rank().equals("NUMBER")
                && card.number() == context.upCard().number()) return true;
        return false;
    }

    static void selfTest() {
        int passed = 0;
        if (Card.of("R5").color() == CardColor.R) passed++; else fail("color R5");
        if (Card.of("G+2").rank().equals("DRAW_TWO")) passed++; else fail("rank +2");
        if (Card.of("W4").points() == 50) passed++; else fail("wild points");
        if (isLegal(Card.of("R2"), new PlayContext(Card.of("R9"), CardColor.NONE))) passed++; else fail("same color");
        if (isLegal(Card.of("G9"), new PlayContext(Card.of("R9"), CardColor.NONE))) passed++; else fail("same number");
        if (isLegal(Card.of("B3"), new PlayContext(Card.of("W"), CardColor.B))) passed++; else fail("called color");
        if (!isLegal(Card.of("B3"), new PlayContext(Card.of("R9"), CardColor.NONE))) passed++; else fail("illegal mismatch");

        ArrayList<Card> h = new ArrayList<Card>();
        h.add(Card.of("B3"));
        h.add(Card.of("R4"));
        h.add(Card.of("W"));
        state.upCard = Card.of("R9");
        state.calledColor = CardColor.NONE;
        if (chooseBotCard(h) == 1) passed++; else fail("bot normal before wild");

        ArrayList<Card> h2 = new ArrayList<Card>();
        h2.add(Card.of("B1"));
        h2.add(Card.of("B2"));
        h2.add(Card.of("R3"));
        if (chooseBotColor(h2) == CardColor.B) passed++; else fail("bot color");

        System.out.println("Passed " + passed + " characterization checks.");

        int newFailures = CharacterizationTests.run();
        if (newFailures > 0) {
            System.exit(1);
        }
    }

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
    }
}
