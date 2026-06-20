package uno;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit wrapper for the characterization tests.
 */
public class CharacterizationTestsTest {

    @Test
    public void testCharacterization() {
        int failures = CharacterizationTests.run();
        assertEquals(0, failures, "Characterization tests should pass");
    }
}