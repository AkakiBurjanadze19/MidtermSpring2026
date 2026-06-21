package uno.persistence.dao;

import uno.persistence.Game;
import uno.persistence.GameScore;
import uno.persistence.Player;
import uno.util.HibernateTestUtil;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDaoTest {

    private static SessionFactory sessionFactory;
    private static GameDao gameDao;
    private static PlayerDao playerDao;

    @BeforeAll
    static void setUp() {
        sessionFactory = HibernateTestUtil.getSessionFactory();
        gameDao = new GameDao(sessionFactory);
        playerDao = new PlayerDao(sessionFactory);
    }

    @Test
    void testSaveAndFindById() {
        Player winner = new Player("Winner");
        playerDao.save(winner);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(3600), 1, winner);
        gameDao.save(game);

        Game found = gameDao.findById(game.getId());
        assertNotNull(found);
        assertEquals(winner.getId(), found.getWinner().getId());
    }

    @Test
    void testSaveWithScores() {
        Player player1 = new Player("Player1");
        Player player2 = new Player("Player2");
        playerDao.save(player1);
        playerDao.save(player2);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, player1);
        GameScore score1 = new GameScore(game, player1, 15);
        GameScore score2 = new GameScore(game, player2, 5);
        List<GameScore> scores = new ArrayList<>();
        scores.add(score1);
        scores.add(score2);

        gameDao.saveWithScores(game, scores);

        // Retrieve the game and verify scores
        Game foundGame = gameDao.findById(game.getId());
        assertNotNull(foundGame);
        assertNotNull(foundGame.getWinner());

        assertEquals(player1.getId(), foundGame.getWinner().getId());
    }

    @Test
    void testFindRecentGames() {
        Player winner = new Player("Recent Winner");
        playerDao.save(winner);

        Game game1 = new Game(Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600), 1, winner);
        Game game2 = new Game(Instant.now(), Instant.now().plusSeconds(1800), 2, winner);

        gameDao.save(game1);
        gameDao.save(game2);

        List<Game> recentGames = gameDao.findRecentGames(1);
        assertEquals(1, recentGames.size());
        // The most recent game should be game2
        assertEquals(game2.getId(), recentGames.get(0).getId());
    }

    @Test
    void testUpdate() {
        Player winner = new Player("Updater");
        playerDao.save(winner);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, winner);
        gameDao.save(game);

        game.setRoundCount(3);
        gameDao.update(game);

        Game found = gameDao.findById(game.getId());
        assertNotNull(found);
        assertEquals(3, found.getRoundCount().intValue());
    }

    @Test
    void testDelete() {
        Player winner = new Player("Deleter");
        playerDao.save(winner);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, winner);
        gameDao.save(game);

        gameDao.delete(game.getId());

        Game found = gameDao.findById(game.getId());
        assertNull(found);
    }
}
