package uno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * playing multiple rounds toward a target
 * score and determining the final winner. Also covers the fixed-round variant.
 */
public class MatchTargetTest {

    @BeforeEach
    void freshQuietMatch() {
        Main.quiet = true;
        Main.state = new GameState();
        Main.setupPlayers(3, false);
        Main.state.setSeed(2026L);
    }

    @Test
    void fixedRoundsModePlaysExactlyThatManyRounds() {
        int rounds = Main.playMatch(3, Main.DEFAULT_TARGET);
        assertEquals(3, rounds);
    }

    @Test
    void targetModeContinuesUntilAPlayerReachesTheTarget() {
        int target = 200;
        int rounds = Main.playMatch(null, target);

        assertTrue(rounds >= 1, "at least one round is played");
        int leader = Main.highestScoringPlayer();
        assertTrue(Main.state.scores[leader] >= target,
                "match only ends once the leader reaches the target");
    }

    @Test
    void finalWinnerIsTheHighestScoringPlayer() {
        Main.playMatch(null, 150);
        int winner = Main.highestScoringPlayer();
        for (int i = 0; i < Main.state.playerNames.size(); i++) {
            assertTrue(Main.state.scores[winner] >= Main.state.scores[i],
                    "winner has the highest score");
        }
    }

    @Test
    void scoresAccumulateAcrossRounds() {
        int afterOne = runOneRoundAndTotal(11L);
        int afterThree = runRoundsAndTotal(11L, 3);
        assertTrue(afterThree >= afterOne, "cumulative score does not decrease across rounds");
    }

    private int runOneRoundAndTotal(long seed) {
        return runRoundsAndTotal(seed, 1);
    }

    private int runRoundsAndTotal(long seed, int rounds) {
        Main.state = new GameState();
        Main.setupPlayers(3, false);
        Main.state.setSeed(seed);
        Main.playMatch(rounds, Main.DEFAULT_TARGET);
        int total = 0;
        for (int i = 0; i < Main.state.playerNames.size(); i++) {
            total += Main.state.scores[i];
        }
        return total;
    }
}
