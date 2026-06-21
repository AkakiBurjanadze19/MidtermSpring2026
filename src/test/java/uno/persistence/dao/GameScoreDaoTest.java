package uno.persistence.dao;

import uno.persistence.Game;
import uno.persistence.GameScore;
import uno.persistence.Player;
import uno.util.HibernateTestUtil;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameScoreDaoTest {

    private static SessionFactory sessionFactory;
    private static GameScoreDao gameScoreDao;
    private static GameDao gameDao;
    private static PlayerDao playerDao;

    @BeforeAll
    static void setUp() {
        sessionFactory = HibernateTestUtil.getSessionFactory();
        gameScoreDao = new GameScoreDao(sessionFactory);
        gameDao = new GameDao(sessionFactory);
        playerDao = new PlayerDao(sessionFactory);
    }

    @Test
    void testSaveAndFindById() {
        Player player = new Player("Score Player");
        playerDao.save(player);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, player);
        gameDao.save(game);

        GameScore score = new GameScore(game, player, 10);
        gameScoreDao.save(score);

        GameScore found = gameScoreDao.findById(score.getId());
        assertNotNull(found);
        assertEquals(player.getId(), found.getPlayer().getId());
        assertEquals(game.getId(), found.getGame().getId());
        assertEquals(10, found.getPoints().intValue());
    }

    @Test
    void testFindByGame() {
        Player player = new Player("Game Score Player");
        playerDao.save(player);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, player);
        gameDao.save(game);

        GameScore score1 = new GameScore(game, player, 8);
        GameScore score2 = new GameScore(game, player, 12);
        gameScoreDao.save(score1);
        gameScoreDao.save(score2);

        var scores = gameScoreDao.findByGame(game);
        assertEquals(2, scores.size());
        // Check that both scores are for the same game
        for (GameScore score : scores) {
            assertEquals(game.getId(), score.getGame().getId());
        }
    }

    @Test
    void testUpdate() {
        Player player = new Player("Updater Score");
        playerDao.save(player);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, player);
        gameDao.save(game);

        GameScore score = new GameScore(game, player, 5);
        gameScoreDao.save(score);

        score.setPoints(20);
        gameScoreDao.update(score);

        GameScore found = gameScoreDao.findById(score.getId());
        assertNotNull(found);
        assertEquals(20, found.getPoints().intValue());
    }

    @Test
    void testDelete() {
        Player player = new Player("Deleter Score");
        playerDao.save(player);

        Game game = new Game(Instant.now(), Instant.now().plusSeconds(1800), 1, player);
        gameDao.save(game);

        GameScore score = new GameScore(game, player, 7);
        gameScoreDao.save(score);

        gameScoreDao.delete(score.getId());

        GameScore found = gameScoreDao.findById(score.getId());
        assertNull(found);
    }
}
