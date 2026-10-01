package org.openl.ie.constrainer.impl;

import java.io.Serial;
import java.io.Serializable;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Expression;
import org.openl.ie.constrainer.ExpressionFactory;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * A generic implementation of the ExpressionFactory interface.
 */
public final class ExpressionFactoryImpl extends UndoableOnceImpl implements ExpressionFactory, Serializable {

    @Serial
    private static final long serialVersionUID = 7593413055525940597L;

    /**
     * Undo Class for UndoExpressionFactory.
     */
    static class UndoExpressionFactory extends UndoImpl {

        static final ReusableFactory FACTORY = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoExpressionFactory();
            }

        };

        static UndoExpressionFactory getUndo() {
            return (UndoExpressionFactory) FACTORY.getElement();
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            return "UndoExpressionFactory " + undoable();
        }

    } // ~UndoExpressionFactory

    /**
     * Returns a constructor with the given parameter types for a given parameter values.
     */
    static Class[] args2types(Object[] args) {
        var size = args.length;
        Class[] types = new Class[size];
        for (var i = 0; i < size; i++) {
            types[i] = args[i].getClass();
        }

        return types;
    }

    /**
     * Default constructor.
     */
    public ExpressionFactoryImpl(Constrainer constrainer) {
        super(constrainer, ExpressionFactoryImpl.class.getName());
    }

    /**
     * Creates a new expression for a given class, args, and types.
     */
    Expression createExpression(Class c, Object[] args, Class[] types) {
        try {
            var constr = c.getConstructor(types);
            constr.setAccessible(true); // to create not public implementations
            return (Expression) constr.newInstance(args);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            var msg = "Error creating expression: " + e.getClass().getName() + ": " + e.getMessage() + ": " + c
                    .getName();

            throw new RuntimeException(msg, e);
        }
    }

    @Override
    public Undo createUndo() {
        return UndoExpressionFactory.getUndo();
    }

    @Override
    public Expression getExpression(Class clazz, Object[] args) {
        return getExpression(clazz, args, args2types(args));
    }

    @Override
    public Expression getExpression(Class clazz, Object[] args, Class[] types) {
        return createExpression(clazz, args, types);
    }

} // ~ExpressionFactoryImpl
