package uno.persistence.dao;

import uno.persistence.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;

import java.util.List;

public class PlayerDao {

    private final SessionFactory sessionFactory;

    // Default constructor for production use
    public PlayerDao() {
        this.sessionFactory = uno.persistence.HibernateUtil.getSessionFactory();
    }

    // Constructor for testing
    public PlayerDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Player player) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(player);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public Player findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Player.class, id);
        }
    }

    public Player findByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Player where name = :name", Player.class)
                    .setParameter("name", name)
                    .uniqueResult();
        }
    }

    public List<Player> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Player", Player.class).list();
        }
    }

    public void update(Player player) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(player);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public void delete(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Player player = session.get(Player.class, id);
            if (player != null) {
                session.remove(player);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
}
