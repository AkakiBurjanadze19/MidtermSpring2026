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

    public void announceGame(int gameNumber) {
        if (Main.quiet) return;
        System.out.println("\n=== Game " + gameNumber + " ===");
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
        System.out.println("Usage: scripts/run.sh [--bots N] [--games N] [--human] [--quiet] [--seed N]");
    }

    public void showError(String message) {
        System.out.println(message);
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
