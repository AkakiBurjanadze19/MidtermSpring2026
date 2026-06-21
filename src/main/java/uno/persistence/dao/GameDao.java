package uno.persistence.dao;

import uno.persistence.Game;
import uno.persistence.GameScore;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;

import java.util.List;

public class GameDao {
    private final SessionFactory sessionFactory;

    // Default construction for production use
    public GameDao() {
        this.sessionFactory = uno.persistence.HibernateUtil.getSessionFactory();
    }

    // Constructor for testing
    public GameDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void save(Game game) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.persist(game);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public void saveWithScores(Game game, List<GameScore> scores) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            // Save the game first to get its ID
            session.persist(game);
            // Now set the game in each score and save them
            for (GameScore score : scores) {
                score.setGame(game);
                session.persist(score);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public Game findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(Game.class, id);
        }
    }

    public List<Game> findRecentGames(int limit) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("from Game order by startTime desc", Game.class)
                    .setMaxResults(limit)
                    .list();
        }
    }

    public void update(Game game) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.merge(game);
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
            Game game = session.get(Game.class, id);
            if (game != null) {
                session.remove(game);
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
