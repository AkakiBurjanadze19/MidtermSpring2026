package uno.persistence.dao;

import uno.persistence.Player;
import uno.util.HibernateTestUtil;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDaoTest {

    private static SessionFactory sessionFactory;
    private static PlayerDao playerDao;

    @BeforeAll
    static void setUp() {
        sessionFactory = HibernateTestUtil.getSessionFactory();
        playerDao = new PlayerDao(sessionFactory);
    }

    @Test
    void testSaveAndFindById() {
        Player player = new Player("Alice");
        playerDao.save(player);

        Player found = playerDao.findById(player.getId());
        assertNotNull(found);
        assertEquals("Alice", found.getName());
    }

    @Test
    void testFindByName() {
        Player player = new Player("Bob");
        playerDao.save(player);

        Player found = playerDao.findByName("Bob");
        assertNotNull(found);
        assertEquals("Bob", found.getName());
    }

    @Test
    void testFindAll() {
        playerDao.save(new Player("Charlie"));
        playerDao.save(new Player("David"));

        var players = playerDao.findAll();
        assertEquals(2, players.size());
    }

    @Test
    void testUpdate() {
        Player player = new Player("Eve");
        playerDao.save(player);

        player.setName("Eve Updated");
        playerDao.update(player);

        Player found = playerDao.findById(player.getId());
        assertNotNull(found);
        assertEquals("Eve Updated", found.getName());
    }

    @Test
    void testDelete() {
        Player player = new Player("Frank");
        playerDao.save(player);

        playerDao.delete(player.getId());

        Player found = playerDao.findById(player.getId());
        assertNull(found);
    }
}
