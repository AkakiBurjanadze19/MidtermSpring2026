package uno;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Console rendering and prompts for the UNO CLI.
 *
 * Every {@code System.out} write and {@code System.in} read used by the game
 * lives here. Rule code in {@link Main} calls methods on a single
 * {@code ConsoleView} instance and never touches stdout or stdin directly.
 *
 * Announcement methods honor {@link Main#quiet}: when quiet is set they are
 * no-ops, matching the previous {@code if (!quiet) System.out.println(...)}
 * pattern. Prompts and the final scoreboard print regardless of quiet,
 * preserving the original behavior.
 *
 * This is the seam for the "replace or improve the CLI view" extension.
 */
public class ConsoleView {

    private final Scanner scanner = new Scanner(System.in);

    // ---------- Announcements (silent when Main.quiet is true) ----------

    public void announceRound(int roundNumber) {
        if (Main.quiet) return;
        System.out.println("\n=== Round " + roundNumber + " ===");
    }

    public void showUpCard(Card upCard, CardColor calledColor) {
        if (Main.quiet) return;
        String suffix = calledColor == CardColor.NONE ? "" : " called " + calledColor.name();
        System.out.println("\nUp card: " + upCard + suffix);
    }

    public void showHand(String playerName, ArrayList<Card> hand) {
        if (Main.quiet) return;
        System.out.println(playerName + " hand: " + join(hand));
    }

    public void announceDraw(String playerName, Card drawnCard) {
        if (Main.quiet) return;
        System.out.println(playerName + " draws " + drawnCard);
    }

    public void announceIllegalIndex(String playerName) {
        if (Main.quiet) return;
        System.out.println(playerName + " selected an invalid index and draws a penalty card.");
    }

    public void announceIllegalPlay(String playerName, Card card) {
        if (Main.quiet) return;
        System.out.println(playerName + " tried illegal card " + card + " and draws a penalty card.");
    }

    public void announcePlay(String playerName, Card card) {
        if (Main.quiet) return;
        System.out.println(playerName + " plays " + card);
    }

    public void announceColorCall(String playerName, CardColor color) {
        if (Main.quiet) return;
        System.out.println(playerName + " calls " + color.name());
    }

    public void announceUno(String playerName) {
        if (Main.quiet) return;
        System.out.println(playerName + " says UNO!");
    }

    public void announceMissedUno(String playerName, int penaltyCards) {
        if (Main.quiet) return;
        System.out.println(playerName + " forgot to call UNO and draws "
                + penaltyCards + " penalty cards.");
    }

    public void announceWin(String playerName, int points) {
        if (Main.quiet) return;
        System.out.println(playerName + " wins and scores " + points);
    }

    public void announceDrawTwo(String victimName) {
        if (Main.quiet) return;
        System.out.println(victimName + " draws two.");
    }

    public void announceDrawFour(String victimName) {
        if (Main.quiet) return;
        System.out.println(victimName + " draws four.");
    }

    public void announceSafetyLimit() {
        if (Main.quiet) return;
        System.out.println("Game stopped at safety limit.");
    }

    // ---------- Unconditional output (printed even with --quiet) ----------

    public void showHelp() {
        System.out.println("UNO CLI");
        System.out.println("Usage: scripts/run.sh [options]");
        System.out.println("  --human         add a human player (default: bots only)");
        System.out.println("  --bots N        number of computer players (default: 3)");
        System.out.println("  --target N      play rounds until a player reaches N points (default: "
                + Main.DEFAULT_TARGET + ")");
        System.out.println("  --games N       instead play exactly N rounds");
        System.out.println("  --seed N        seed the shuffler for reproducible games");
        System.out.println("  --quiet         suppress per-turn narration");
        System.out.println("  --self-test     run the built-in checks and exit");
        System.out.println("  --help          show this message");
        System.out.println("Total players (human + bots) must be between 2 and 4.");
    }

    public void showError(String message) {
        System.out.println(message);
    }

    public void showStandings(ArrayList<String> playerNames, int[] scores) {
        if (Main.quiet) return;
        StringBuilder line = new StringBuilder("Standings:");
        for (int i = 0; i < playerNames.size(); i++) {
            line.append(' ').append(playerNames.get(i)).append('=').append(scores[i]);
        }
        System.out.println(line);
    }

    public void announceMatchWinner(String playerName, int score) {
        System.out.println("\n" + playerName + " wins the match with " + score + " points!");
    }

    public void showFinalScores(ArrayList<String> playerNames, int[] scores) {
        System.out.println("\nFinal scores:");
        for (int i = 0; i < playerNames.size(); i++) {
            System.out.println(playerNames.get(i) + ": " + scores[i]);
        }
    }

    // ---------- Prompts (only invoked on human turns) ----------

    public boolean promptPlayDrawnCard(Card drawn) {
        System.out.print("Play drawn card " + drawn + "? y/n: ");
        String answer = scanner.nextLine();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    public String promptCardChoice() {
        System.out.print("Choose card index/code or draw: ");
        return scanner.nextLine().trim().toUpperCase();
    }

    public String promptColor() {
        System.out.print("Call color R/Y/G/B: ");
        return scanner.nextLine().trim().toUpperCase();
    }

    /** Ask a human player who just reached one card whether they call "UNO!". */
    public boolean promptCallUno() {
        System.out.print("You are down to one card. Call UNO? y/n: ");
        String answer = scanner.nextLine();
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }

    public void notify(String message) {
        System.out.println(message);
    }

    // ---------- Formatting ----------

    private static String join(ArrayList<Card> cards) {
        String out = "";
        for (int i = 0; i < cards.size(); i++) {
            out += i + ":" + cards.get(i);
            if (i < cards.size() - 1) {
                out += " ";
            }
        }
        return out;
    }
}
