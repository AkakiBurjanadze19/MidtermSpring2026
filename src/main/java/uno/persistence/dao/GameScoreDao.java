package uno.persistence.dao;

import uno.persistence.Game;
import uno.persistence.GameScore;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;

import java.util.List;

public class GameScoreDao {

    private final SessionFactory sessionFactory;

    // Default constructor for production use
    public GameScoreDao() {
        this.sessionFactory = uno.persistence.HibernateUtil.getSessionFactory();
    }

    // Constructor for testing
    public GameScoreDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(GameScore score) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(score);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public GameScore findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(GameScore.class, id);
        }
    }

    public List<GameScore> findByGame(Game game) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from GameScore where game = :game", GameScore.class)
                    .setParameter("game", game)
                    .list();
        }
    }

    public void update(GameScore score) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(score);
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
            GameScore score = session.get(GameScore.class, id);
            if (score != null) {
                session.remove(score);
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
