package uno.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

public class HibernateTestUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Create the ServiceRegistry from hibernate.cfg.xml (test version)
            StandardServiceRegistry standardRegistry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml")
                    .build();

            // Create the metadata sources
            MetadataSources metadataSources = new MetadataSources(standardRegistry);

            Metadata metadata = metadataSources.getMetadataBuilder()
                    .build();

            // Create the SessionFactory
            return metadata.getSessionFactoryBuilder()
                    .build();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed (test)." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
    }
}
