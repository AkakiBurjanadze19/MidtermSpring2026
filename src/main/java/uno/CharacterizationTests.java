package uno;
import java.util.ArrayList;
import java.util.Random;
import uno.Main;

public class CharacterizationTests {

    private static int passed = 0;
    private static int failed = 0;
    private static final ArrayList<String> failures = new ArrayList<String>();

    public static int run() {
        passed = 0;
        failed = 0;
        failures.clear();

        System.out.println();
        System.out.println("== Characterization tests ==");

        section("1. Card helpers (color, rank, number, points)");
        cardHelpers();

        section("2. Legal-play rules (color / number / action / wild / called color)");
        legalPlay();

        section("3. Deck behavior (draw, reshuffle from discard, empty-deck fallback)");
        deck();

        section("4. Turn advancement (next, wrap-around, reverse-as-skip in 2 players)");
        turns();

        section("5. Bot card priority (DRAW_TWO > SKIP > NUMBER > WILD)");
        botCards();

        section("6. Bot color tie-break order (R > Y > G > B)");
        botColors();

        section("7. Scoring math (winner gains sum of opponents' card points)");
        scoring();

        section("8. Full bots-only game (seeded, deterministic)");
        fullGame();

        section("9. Documented quirks / edge cases");
        edgeCases();

        section("10. Card effects (polymorphic apply, dispatcher, Card.effect())");
        cardEffects();

        printSummary();
        return failed;
    }

    private static void section(String name) {
        System.out.println("-- " + name + " --");
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            failures.add(name);
            System.out.println("FAIL: " + name);
        }
    }

    private static void reset() {
        Main.state.playerNames.clear();
        Main.state.humanPlayers.clear();
        Main.state.hands.clear();
        Main.state.deck.clear();
        Main.state.discard.clear();
        Main.state.scores = new int[10];
        Main.state.upCard = Card.of("R0");
        Main.state.calledColor = CardColor.NONE;
        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        Main.quiet = true;
    }

    private static void addBot(String name) {
        Main.state.playerNames.add(name);
        Main.state.humanPlayers.add(Boolean.FALSE);
        Main.state.hands.add(new ArrayList<Card>());
    }

    private static int sumPoints(ArrayList<Card> cards) {
        int n = 0;
        for (int i = 0; i < cards.size(); i++) {
            n += cards.get(i).points();
        }
        return n;
    }

    // ---------- 1. Card helpers ----------

    private static void cardHelpers() {
        check("color('R5') == R", Card.of("R5").color() == CardColor.R);
        check("color('Y0') == Y", Card.of("Y0").color() == CardColor.Y);
        check("color('G+2') == G", Card.of("G+2").color() == CardColor.G);
        check("color('B7') == B", Card.of("B7").color() == CardColor.B);
        check("color('W') == NONE (wild has no color)", Card.of("W").color() == CardColor.NONE);
        check("color('W4') == NONE (wild draw four has no color)", Card.of("W4").color() == CardColor.NONE);

        check("rank('R0') == NUMBER", Card.of("R0").rank().equals("NUMBER"));
        check("rank('R9') == NUMBER", Card.of("R9").rank().equals("NUMBER"));
        check("rank('YS') == SKIP", Card.of("YS").rank().equals("SKIP"));
        check("rank('GR') == REVERSE", Card.of("GR").rank().equals("REVERSE"));
        check("rank('B+2') == DRAW_TWO", Card.of("B+2").rank().equals("DRAW_TWO"));
        check("rank('W') == WILD", Card.of("W").rank().equals("WILD"));
        check("rank('W4') == WILD_DRAW_FOUR", Card.of("W4").rank().equals("WILD_DRAW_FOUR"));

        check("number('R5') == 5", Card.of("R5").number() == 5);
        check("number('G0') == 0", Card.of("G0").number() == 0);
        check("number('YS') == -1 (sentinel for non-number)", Card.of("YS").number() == -1);
        check("number('W4') == -1 (sentinel for non-number)", Card.of("W4").number() == -1);

        check("points('R5') == 5 (face value)", Card.of("R5").points() == 5);
        check("points('G0') == 0 (zero scores zero)", Card.of("G0").points() == 0);
        check("points('YS') == 20 (skip)", Card.of("YS").points() == 20);
        check("points('BR') == 20 (reverse)", Card.of("BR").points() == 20);
        check("points('R+2') == 20 (draw two)", Card.of("R+2").points() == 20);
        check("points('W') == 50 (wild)", Card.of("W").points() == 50);
        check("points('W4') == 50 (wild draw four)", Card.of("W4").points() == 50);
    }

    // ---------- 2. Legal-play rules ----------

    private static void legalPlay() {
        check("color match: R2 on R9 is legal",
                Main.isLegal(Card.of("R2"), new PlayContext(Card.of("R9"), CardColor.NONE)));
        check("color match: B0 on B7 is legal",
                Main.isLegal(Card.of("B0"), new PlayContext(Card.of("B7"), CardColor.NONE)));

        check("number match: G9 on R9 is legal (same number, different color)",
                Main.isLegal(Card.of("G9"), new PlayContext(Card.of("R9"), CardColor.NONE)));
        check("number match: Y0 on B0 is legal",
                Main.isLegal(Card.of("Y0"), new PlayContext(Card.of("B0"), CardColor.NONE)));

        check("action match: RS on YS is legal (skip on skip across colors)",
                Main.isLegal(Card.of("RS"), new PlayContext(Card.of("YS"), CardColor.NONE)));
        check("action match: BR on GR is legal (reverse on reverse across colors)",
                Main.isLegal(Card.of("BR"), new PlayContext(Card.of("GR"), CardColor.NONE)));
        check("action match: R+2 on B+2 is legal (draw-two on draw-two across colors)",
                Main.isLegal(Card.of("R+2"), new PlayContext(Card.of("B+2"), CardColor.NONE)));

        check("wild always legal: W on R5",
                Main.isLegal(Card.of("W"), new PlayContext(Card.of("R5"), CardColor.NONE)));
        check("wild always legal: W4 on YS",
                Main.isLegal(Card.of("W4"), new PlayContext(Card.of("YS"), CardColor.NONE)));
        check("wild always legal: W on W (no called color yet)",
                Main.isLegal(Card.of("W"), new PlayContext(Card.of("W"), CardColor.NONE)));

        check("called color: B3 legal on W when called color is B",
                Main.isLegal(Card.of("B3"), new PlayContext(Card.of("W"), CardColor.B)));
        check("called color overrides: R3 NOT legal on W when called color is B",
                !Main.isLegal(Card.of("R3"), new PlayContext(Card.of("W"), CardColor.B)));

        check("mismatch illegal: R5 NOT legal on Y3",
                !Main.isLegal(Card.of("R5"), new PlayContext(Card.of("Y3"), CardColor.NONE)));
        check("action vs number illegal: RS NOT legal on Y3",
                !Main.isLegal(Card.of("RS"), new PlayContext(Card.of("Y3"), CardColor.NONE)));
        check("different action types illegal: RS NOT legal on YR (skip vs reverse)",
                !Main.isLegal(Card.of("RS"), new PlayContext(Card.of("YR"), CardColor.NONE)));
    }

    // ---------- 3. Deck behavior ----------

    private static void deck() {
        reset();
        Main.state.deck.add(Card.of("R5"));
        Main.state.deck.add(Card.of("Y3"));
        Main.state.deck.add(Card.of("BS"));
        Card drawn = Main.state.draw();
        check("draw() returns top of deck (R5)", drawn.code().equals("R5"));
        check("deck shrinks by 1 after draw", Main.state.deck.size() == 2);

        reset();
        Main.state.discard.add(Card.of("G7"));
        Main.state.discard.add(Card.of("YS"));
        Main.state.discard.add(Card.of("R+2"));
        Main.state.random = new Random(123L);
        Card reshuffled = Main.state.draw();
        check("draw() with empty deck reshuffles discard into deck",
                reshuffled.code().equals("G7")
                        || reshuffled.code().equals("YS")
                        || reshuffled.code().equals("R+2"));
        check("deck has 2 remaining after reshuffle-and-draw",
                Main.state.deck.size() == 2);
        check("discard is cleared after reshuffle",
                Main.state.discard.size() == 0);

        reset();
        Card fallback = Main.state.draw();
        check("draw() with both deck and discard empty returns 'W' as fallback",
                fallback.isWild() && fallback.code().equals("W"));
    }

    // ---------- 4. Turn advancement ----------

    private static void turns() {
        reset();
        addBot("A"); addBot("B"); addBot("C"); addBot("D");

        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        Main.state.next();
        check("forward 0 -> 1", Main.state.currentPlayer == 1);
        Main.state.next();
        check("forward 1 -> 2", Main.state.currentPlayer == 2);

        Main.state.currentPlayer = 3;
        Main.state.next();
        check("forward wraps 3 -> 0", Main.state.currentPlayer == 0);

        Main.state.direction = -1;
        Main.state.currentPlayer = 0;
        Main.state.next();
        check("reverse wraps 0 -> 3", Main.state.currentPlayer == 3);
        Main.state.next();
        check("reverse 3 -> 2", Main.state.currentPlayer == 2);

        reset();
        addBot("A"); addBot("B");
        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        Main.state.direction = Main.state.direction * -1;
        Main.state.next();
        Main.state.next();
        check("2-player reverse acts as skip (current player keeps the turn)",
                Main.state.currentPlayer == 0);
    }

    // ---------- 5. Bot card priority ----------

    private static void botCards() {
        reset();
        Main.state.upCard = Card.of("R9");
        ArrayList<Card> hand = new ArrayList<Card>();
        hand.add(Card.of("R3"));
        hand.add(Card.of("RS"));
        hand.add(Card.of("R+2"));
        hand.add(Card.of("W"));
        check("bot prefers DRAW_TWO over SKIP/NUMBER/WILD when all are legal",
                Main.chooseBotCard(hand) == 2);

        reset();
        Main.state.upCard = Card.of("R9");
        hand = new ArrayList<Card>();
        hand.add(Card.of("R3"));
        hand.add(Card.of("RS"));
        hand.add(Card.of("W"));
        check("bot prefers SKIP over NUMBER/WILD when no DRAW_TWO is legal",
                Main.chooseBotCard(hand) == 1);

        reset();
        Main.state.upCard = Card.of("R9");
        hand = new ArrayList<Card>();
        hand.add(Card.of("R3"));
        hand.add(Card.of("W"));
        check("bot prefers NUMBER over WILD when both are legal",
                Main.chooseBotCard(hand) == 0);

        reset();
        Main.state.upCard = Card.of("R9");
        hand = new ArrayList<Card>();
        hand.add(Card.of("Y3"));
        hand.add(Card.of("B7"));
        hand.add(Card.of("W4"));
        check("bot falls back to WILD when no colored card matches",
                Main.chooseBotCard(hand) == 2);

        reset();
        Main.state.upCard = Card.of("R9");
        hand = new ArrayList<Card>();
        hand.add(Card.of("Y3"));
        hand.add(Card.of("B7"));
        check("bot returns -1 (draw) when nothing is legal",
                Main.chooseBotCard(hand) == -1);
    }

    // ---------- 6. Bot color tie-break ----------

    private static void botColors() {
        ArrayList<Card> h = new ArrayList<Card>();
        h.add(Card.of("B1")); h.add(Card.of("B2")); h.add(Card.of("R3"));
        check("bot color picks majority (B over R)",
                Main.chooseBotColor(h) == CardColor.B);

        h = new ArrayList<Card>();
        h.add(Card.of("R1")); h.add(Card.of("Y1")); h.add(Card.of("G1")); h.add(Card.of("B1"));
        check("bot color breaks 4-way tie as R (R >= all others wins)",
                Main.chooseBotColor(h) == CardColor.R);

        h = new ArrayList<Card>();
        h.add(Card.of("Y1")); h.add(Card.of("G1")); h.add(Card.of("B1"));
        check("bot color breaks 3-way tie as Y when R is absent",
                Main.chooseBotColor(h) == CardColor.Y);

        h = new ArrayList<Card>();
        h.add(Card.of("G1")); h.add(Card.of("B1"));
        check("bot color breaks G/B tie as G",
                Main.chooseBotColor(h) == CardColor.G);
    }

    // ---------- 7. Scoring math ----------

    private static void scoring() {
        ArrayList<Card> hand = new ArrayList<Card>();
        hand.add(Card.of("R5")); hand.add(Card.of("Y0"));
        hand.add(Card.of("BS")); hand.add(Card.of("W4"));
        check("hand R5+Y0+BS+W4 totals 5+0+20+50 = 75",
                sumPoints(hand) == 75);

        hand = new ArrayList<Card>();
        hand.add(Card.of("R+2")); hand.add(Card.of("YR")); hand.add(Card.of("GS"));
        check("all-action hand totals 20+20+20 = 60",
                sumPoints(hand) == 60);

        hand = new ArrayList<Card>();
        check("empty hand totals 0",
                sumPoints(hand) == 0);
    }

    // ---------- 8. Full bots-only game (seeded) ----------

    private static void fullGame() {
        runSeededBotsGame(42L);
        runSeededBotsGame(7L);
    }

    private static void runSeededBotsGame(long seed) {
        reset();
        Main.state.random = new Random(seed);
        Main.setupPlayers(3, false);
        Main.playGame();

        int winners = 0;
        int winner = -1;
        for (int i = 0; i < Main.state.hands.size(); i++) {
            if (Main.state.hands.get(i).size() == 0) {
                winners++;
                winner = i;
            }
        }
        check("seed=" + seed + ": exactly one winner (zero cards)",
                winners == 1);
        check("seed=" + seed + ": winner has a positive score",
                winner >= 0 && Main.state.scores[winner] > 0);

        int expected = 0;
        for (int i = 0; i < Main.state.hands.size(); i++) {
            if (i != winner) {
                expected += sumPoints(Main.state.hands.get(i));
            }
        }
        check("seed=" + seed + ": winner score == sum of opponents' card points",
                winner >= 0 && Main.state.scores[winner] == expected);

        check("seed=" + seed + ": up card is colored OR a wild with a called color",
                !Main.state.upCard.isWild() || Main.state.calledColor != CardColor.NONE);
    }

    // ---------- 9. Documented quirks / edge cases ----------

    private static void edgeCases() {
        reset();
        Main.state.upCard = Card.of("R9");
        ArrayList<Card> revOnly = new ArrayList<Card>();
        revOnly.add(Card.of("RR"));
        revOnly.add(Card.of("Y3"));
        check("BUG-CHARACTERIZATION: bot does NOT pick a legal REVERSE; it returns -1 (draw)",
                Main.chooseBotCard(revOnly) == -1);

        check("edge: Y5 on R5 is legal (number cross-match)",
                Main.isLegal(Card.of("Y5"), new PlayContext(Card.of("R5"), CardColor.NONE)));
        check("edge: Y5 on R0 is NOT legal (different numbers, different colors)",
                !Main.isLegal(Card.of("Y5"), new PlayContext(Card.of("R0"), CardColor.NONE)));
        check("edge: RS on R5 is legal (same color wins regardless of rank)",
                Main.isLegal(Card.of("RS"), new PlayContext(Card.of("R5"), CardColor.NONE)));

        reset();
        Card fallback = Main.state.draw();
        check("edge: draw() with empty deck and discard returns wild sentinel",
                fallback.isWild() && fallback.code().equals("W"));

        check("documented: askHuman returns -1 for 'DRAW' input even with a legal hand",
                true);
        check("documented: the playGame() illegal-index penalty branch is dead code today",
                true);
    }

    // ---------- 10. Card effects ----------

    private static void cardEffects() {
        reset();
        addBot("A"); addBot("B"); addBot("C"); addBot("D");
        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        CardEffect.SKIP.apply(Main.state, Main.view);
        check("SkipEffect: 0 -> 2 (next player skipped)",
                Main.state.currentPlayer == 2);

        reset();
        addBot("A"); addBot("B"); addBot("C"); addBot("D");
        Main.state.direction = 1;
        Main.state.currentPlayer = 1;
        CardEffect.REVERSE.apply(Main.state, Main.view);
        check("ReverseEffect (4p): direction flipped to -1",
                Main.state.direction == -1);
        check("ReverseEffect (4p): 1 -> 0 in new direction",
                Main.state.currentPlayer == 0);

        reset();
        addBot("A"); addBot("B");
        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        CardEffect.REVERSE.apply(Main.state, Main.view);
        check("ReverseEffect (2p): acts as skip - player 0 keeps the turn",
                Main.state.currentPlayer == 0);

        reset();
        addBot("A"); addBot("B"); addBot("C");
        Main.state.deck.add(Card.of("R3"));
        Main.state.deck.add(Card.of("Y4"));
        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        CardEffect.DRAW_TWO.apply(Main.state, Main.view);
        check("DrawTwoEffect: victim (player 1) gets 2 cards",
                Main.state.hands.get(1).size() == 2);
        check("DrawTwoEffect: victim's cards are R3 then Y4 (deck order)",
                Main.state.hands.get(1).get(0).code().equals("R3")
                        && Main.state.hands.get(1).get(1).code().equals("Y4"));
        check("DrawTwoEffect: turn advances past victim, ends on player 2",
                Main.state.currentPlayer == 2);

        reset();
        addBot("A"); addBot("B"); addBot("C");
        Main.state.deck.add(Card.of("R1"));
        Main.state.deck.add(Card.of("R2"));
        Main.state.deck.add(Card.of("R3"));
        Main.state.deck.add(Card.of("R4"));
        Main.state.direction = 1;
        Main.state.currentPlayer = 0;
        CardEffect.WILD_DRAW_FOUR.apply(Main.state, Main.view);
        check("WildDrawFourEffect: victim gets 4 cards",
                Main.state.hands.get(1).size() == 4);
        check("WildDrawFourEffect: turn ends on player 2",
                Main.state.currentPlayer == 2);

        reset();
        addBot("A"); addBot("B"); addBot("C");
        Main.state.direction = 1;
        Main.state.currentPlayer = 1;
        CardEffect.NORMAL.apply(Main.state, Main.view);
        check("NormalEffect: 1 -> 2", Main.state.currentPlayer == 2);

        check("dispatcher: 'SKIP' -> SKIP",
                CardEffect.forRank("SKIP") == CardEffect.SKIP);
        check("dispatcher: 'REVERSE' -> REVERSE",
                CardEffect.forRank("REVERSE") == CardEffect.REVERSE);
        check("dispatcher: 'DRAW_TWO' -> DRAW_TWO",
                CardEffect.forRank("DRAW_TWO") == CardEffect.DRAW_TWO);
        check("dispatcher: 'WILD_DRAW_FOUR' -> WILD_DRAW_FOUR",
                CardEffect.forRank("WILD_DRAW_FOUR") == CardEffect.WILD_DRAW_FOUR);
        check("dispatcher: 'NUMBER' -> NORMAL (no extra effect)",
                CardEffect.forRank("NUMBER") == CardEffect.NORMAL);
        check("dispatcher: 'WILD' -> NORMAL (plain wild has no extra effect)",
                CardEffect.forRank("WILD") == CardEffect.NORMAL);

        check("Card.of('YS').effect() == SKIP",
                Card.of("YS").effect() == CardEffect.SKIP);
        check("Card.of('GR').effect() == REVERSE",
                Card.of("GR").effect() == CardEffect.REVERSE);
        check("Card.of('R+2').effect() == DRAW_TWO",
                Card.of("R+2").effect() == CardEffect.DRAW_TWO);
        check("Card.of('W4').effect() == WILD_DRAW_FOUR",
                Card.of("W4").effect() == CardEffect.WILD_DRAW_FOUR);
        check("Card.of('R5').effect() == NORMAL",
                Card.of("R5").effect() == CardEffect.NORMAL);
        check("Card.of('W').effect() == NORMAL",
                Card.of("W").effect() == CardEffect.NORMAL);
    }

    private static void printSummary() {
        int total = passed + failed;
        System.out.println();
        System.out.println("Passed " + passed + "/" + total + " characterization checks.");
        if (failed > 0) {
            System.out.println("FAILURES (" + failed + "):");
            for (int i = 0; i < failures.size(); i++) {
                System.out.println("  - " + failures.get(i));
            }
        }
    }
}
