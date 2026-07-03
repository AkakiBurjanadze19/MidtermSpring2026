package uno;

import java.util.ArrayList;
import java.time.Instant;
import uno.persistence.Game;
import uno.persistence.GameScore;
import uno.persistence.Player;
import uno.persistence.dao.GameDao;
import uno.persistence.dao.PlayerDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller / entry point for the UNO CLI.
 *
 * <p>{@code Main} owns orchestration only: it parses CLI arguments, runs the
 * match / round / turn loops, and wires the collaborators together. The actual
 * rules live elsewhere and are testable without a console:
 *
 * <ul>
 *   <li>{@link Rules} – legal-play validation and round scoring;</li>
 *   <li>{@link CardEffect} – the state change each played card causes;</li>
 *   <li>{@link BotStrategy} – computer-player decisions;</li>
 *   <li>{@link GameState} – all mutable game data and deck operations;</li>
 *   <li>{@link ConsoleView} – every read from stdin and write to stdout.</li>
 * </ul>
 *
 * <p>A <em>match</em> is a series of <em>rounds</em>. Each round is one hand of
 * UNO that ends when a player empties their hand; that player scores the point
 * value of every card left in the other hands. By default the match continues
 * until a player reaches the target score (500); {@code --games N} instead
 * plays a fixed number of rounds.
 */
public class Main {

    static GameState state = new GameState();
    static boolean quiet = false;
    static ConsoleView view = new ConsoleView();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** Default target score for a play-to-target match, as in classic UNO. */
    static final int DEFAULT_TARGET = 500;

    /** Hard cap on rounds per match so a pathological match cannot loop forever. */
    private static final int MAX_ROUNDS = 1000;

    // Initialize Data Access Object (DAO) instances
    private static final PlayerDao playerDao = new PlayerDao();
    private static final GameDao gameDao = new GameDao();

    public static void main(String[] args) {
        int bots = 3;
        Integer fixedRounds = null;   // set by --games; null means "play to target"
        int target = DEFAULT_TARGET;  // set by --target
        boolean human = false;
        long seed = System.currentTimeMillis();

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--bots") && i + 1 < args.length) {
                    bots = Integer.parseInt(args[++i]);
                } else if (args[i].equals("--games") && i + 1 < args.length) {
                    fixedRounds = Integer.parseInt(args[++i]);
                } else if (args[i].equals("--target") && i + 1 < args.length) {
                    target = Integer.parseInt(args[++i]);
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
                } else {
                    view.showError("Unknown or incomplete option: " + args[i]);
                    view.showHelp();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            view.showError("Numeric options (--bots/--games/--target/--seed) need a number.");
            view.showHelp();
            return;
        }

        state.setSeed(seed);
        setupPlayers(bots, human);

        if (state.playerNames.size() < 2 || state.playerNames.size() > 4) {
            view.showError("UNO needs 2 to 4 players (use --bots and/or --human).");
            return;
        }

        Instant matchStartTime = Instant.now();
        int roundsPlayed = playMatch(fixedRounds, target);
        Instant matchEndTime = Instant.now();

        int winningPlayerIndex = highestScoringPlayer();
        String winnerName = state.playerNames.get(winningPlayerIndex);

        view.showFinalScores(state.playerNames, state.scores);
        view.announceMatchWinner(winnerName, state.scores[winningPlayerIndex]);
        logger.info("Match completed in {} round(s). Winner: {} ({} pts).",
                roundsPlayed, winnerName, state.scores[winningPlayerIndex]);

        persistMatch(matchStartTime, matchEndTime, roundsPlayed, winnerName);
    }

    /**
     * Play a full match and return the number of rounds played. When
     * {@code fixedRounds} is non-null the match runs exactly that many rounds;
     * otherwise it runs until a player reaches {@code target} points.
     */
    static int playMatch(Integer fixedRounds, int target) {
        int round = 0;
        while (round < MAX_ROUNDS) {
            round++;
            view.announceRound(round);
            logger.info("Starting round {}", round);
            playRound();
            view.showStandings(state.playerNames, state.scores);

            if (fixedRounds != null) {
                if (round >= fixedRounds) {
                    break;
                }
            } else if (state.scores[highestScoringPlayer()] >= target) {
                break;
            }
        }
        return round;
    }

    /** The index of the player with the highest cumulative score. */
    static int highestScoringPlayer() {
        int best = 0;
        for (int i = 1; i < state.playerNames.size(); i++) {
            if (state.scores[i] > state.scores[best]) {
                best = i;
            }
        }
        return best;
    }

    /** Persist the finished match; never lets a storage failure crash the game. */
    private static void persistMatch(Instant start, Instant end, int rounds, String winnerName) {
        try {
            for (String name : state.playerNames) {
                if (playerDao.findByName(name) == null) {
                    playerDao.save(new Player(name));
                }
            }
            Player winner = playerDao.findByName(winnerName);
            Game match = new Game(start, end, rounds, winner);
            gameDao.saveWithScores(match, createMatchScores(match));
            uno.persistence.HibernateUtil.shutdown();
        } catch (Throwable t) {
            logger.warn("Skipping match persistence: {}", t.toString());
        }
    }

    /**
     * Create GameScore objects for each player based on their total scores in the match.
     */
    private static ArrayList<GameScore> createMatchScores(Game match) {
        ArrayList<GameScore> scores = new ArrayList<>();
        for (int i = 0; i < state.playerNames.size(); i++) {
            Player player = playerDao.findByName(state.playerNames.get(i));
            scores.add(new GameScore(match, player, state.scores[i]));
        }
        return scores;
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

    /** Set up and play a single round; scores are added to {@link GameState#scores}. */
    static void playRound() {
        state.buildStandardDeck();
        state.shuffleDeck();
        state.discard.clear();
        state.dealStartingHands(7);
        state.flipInitialUpCard();
        state.calledColor = CardColor.NONE;
        state.direction = 1;
        state.saidUno = new boolean[state.scores.length];
        state.currentPlayer = state.random.nextInt(state.playerNames.size());

        runTurnLoop();
    }

    /** Backwards-compatible alias for {@link #playRound()} used by tests. */
    static void playGame() {
        playRound();
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

    /** One turn for the current player. Returns true if the round has ended. */
    static boolean playSingleTurn() {
        String name = state.playerNames.get(state.currentPlayer);
        logger.info("Turn for player: {}", name);
        ArrayList<Card> hand = state.hands.get(state.currentPlayer);

        view.showUpCard(state.upCard, state.calledColor);
        view.showHand(name, hand);

        int chosen = pickCardOrDraw(hand, name);
        if (chosen < 0) {
            state.next();
            return false;
        }

        if (!isValidPlay(chosen, hand, name)) {
            logger.warn("Invalid play attempted by {}", name);
            Card drawn = state.draw();
            hand.add(drawn);
            logger.info("Card drawn: {} for {} (invalid play penalty)", drawn, name);
            state.next();
            return false;
        }

        Card card = applyPlay(chosen, hand, name);

        if (handleWinIfAny(name)) {
            logger.info("Round won by {}", name);
            return true;
        }

        card.effect().apply(state, view);
        return false;
    }

    /**
     * Ask the current player for a card index, or draw one and decide whether
     * to auto-play it. Returns the index to play, or -1 to pass the turn.
     *
     * <p>Draw/pass rule: a player with no legal play draws exactly one card. If
     * that card is legal they may play it immediately (bots always do, a human
     * is asked); otherwise the turn passes.
     */
    static int pickCardOrDraw(ArrayList<Card> hand, String name) {
        int chosen = isCurrentHuman() ? askHuman(hand) : chooseBotCard(hand);
        if (chosen != -1) {
            return chosen;
        }

        Card drawn = state.draw();
        hand.add(drawn);
        view.announceDraw(name, drawn);
        logger.info("Card drawn: {} for {}", drawn, name);

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
     * up-card, ask for a called color on wilds, then handle the UNO call.
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
            handleUnoCall(name);
        }
        logger.info("Card played: {} by {}", card, name);
        return card;
    }

    /**
     * Resolve the UNO call for the current player, who has just reached one
     * card. If they call UNO they are safe; if they fail to call before the
     * turn passes on, they are immediately caught and draw a penalty.
     */
    static void handleUnoCall(String name) {
        int player = state.currentPlayer;
        boolean called = isCurrentHuman() ? view.promptCallUno() : BotStrategy.shouldCallUno();
        if (called) {
            state.callUno(player);
            view.announceUno(name);
            logger.info("{} called UNO", name);
        } else {
            int drawn = state.applyMissedUnoPenalty(player);
            view.announceMissedUno(name, drawn);
            logger.info("{} failed to call UNO and drew {} penalty cards", name, drawn);
        }
    }

    /**
     * If the current player has emptied their hand, credit them with opponents'
     * point total, announce the win, and return true.
     */
    static boolean handleWinIfAny(String name) {
        if (state.hands.get(state.currentPlayer).size() != 0) {
            return false;
        }
        int points = Rules.scoreForWinner(state, state.currentPlayer);
        state.scores[state.currentPlayer] += points;
        view.announceWin(name, points);
        return true;
    }

    static boolean isCurrentHuman() {
        return state.humanPlayers.get(state.currentPlayer).booleanValue();
    }

    static int chooseBotCard(ArrayList<Card> hand) {
        return BotStrategy.chooseCard(hand, PlayContext.of(state));
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
                logger.info("Non-numeric input: {}", input);
            }
            PlayContext context = PlayContext.of(state);
            boolean found = false;
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).code().equals(input)) {
                    found = true;
                    if (isLegal(hand.get(i), context)) {
                        return i;
                    }
                    view.notify("That card is not legal.");
                    logger.info("Illegal card attempted: {}", input);
                }
            }
            if (!found) {
                view.notify("Card not found. Enter an index, a card code (e.g. R5), or DRAW.");
                logger.info("Card not found: {}", input);
            }
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
            view.notify("Bad color. Choose R, Y, G, or B.");
        }
    }

    static CardColor chooseBotColor(ArrayList<Card> hand) {
        return BotStrategy.chooseColor(hand);
    }

    /** @see Rules#isLegal(Card, PlayContext) */
    static boolean isLegal(Card card, PlayContext context) {
        return Rules.isLegal(card, context);
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
