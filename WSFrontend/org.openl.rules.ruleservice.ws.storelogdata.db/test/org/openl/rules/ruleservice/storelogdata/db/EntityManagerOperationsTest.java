package org.openl.rules.ruleservice.storelogdata.db;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Strict.class)
public class EntityManagerOperationsTest {

    @InjectMocks
    private EntityManagerOperations entityManagerOperations;

    @Mock
    private HibernateSessionFactoryBuilder hibernateSessionFactoryBuilder;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Before
    public void setUp() {
        when(hibernateSessionFactoryBuilder.buildSessionFactory(any())).thenReturn(sessionFactory);
        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
    }

    @Test
    public void testCheckClosedConnectionWhenExceptionAndNotActiveTx() {
        var entity = new Object();
        when(session.merge(any())).thenThrow(new PersistenceException());

        assertThrows(PersistenceException.class,
            () -> entityManagerOperations.save(new Class<?>[] { Object.class }, entity));

        verify(transaction, never()).rollback();
        verify(transaction, never()).commit();

        verify(session).beginTransaction();
        verify(session).merge(entity);
        verify(session).close();
    }

    @Test
    public void testCheckClosedConnectionWhenExceptionAndActiveTx() {
        var entity = new Object();
        when(session.merge(any())).thenThrow(new PersistenceException());
        when(transaction.isActive()).thenReturn(Boolean.TRUE);

        assertThrows(PersistenceException.class,
            () -> entityManagerOperations.save(new Class<?>[] { Object.class }, entity));

        // make sure that tx rolled back before connection is closed
        var inOrder = inOrder(transaction, session);
        inOrder.verify(transaction).rollback();
        inOrder.verify(session).close();

        verify(transaction, never()).commit();

        verify(session).beginTransaction();
        verify(session).merge(entity);
    }

    @Test
    public void testSuccessSave() {
        var entity = new Object();
        entityManagerOperations.save(new Class<?>[] { Object.class }, entity);

        verify(transaction, never()).rollback();
        verify(transaction).commit();

        verify(session).beginTransaction();
        verify(session).merge(entity);
        verify(session).close();
    }
}
