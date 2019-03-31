package org.openl.ie.constrainer.impl;

import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Expression;
import org.openl.ie.constrainer.ExpressionFactory;
import org.openl.ie.constrainer.FloatExpArray;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.constrainer.Undoable;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * A generic implementation of the ExpressionFactory interface.
 */
public final class ExpressionFactoryImpl extends UndoableOnceImpl implements ExpressionFactory, java.io.Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 7593413055525940597L;

    /**
     * An interface for the unique key of the expression.
     */
    interface ExpressionKey {
        /**
         * Returns the arguments of the expression.
         */
        public Object[] args();

        /**
         * Returns a class of the expression.
         */
        public Class clazz();
    }

    /**
     *
     */
    static class ExpressionKeyImpl implements ExpressionKey {
        private Class _clazz;
        private Object[] _args;

        static boolean equalArgs(Object arg1, Object arg2) {
            // are references the same?
            if (arg1 == arg2) {
                return true; // yes -> equal
            }

            // are classes the same?
            if (arg1.getClass() != arg2.getClass()) {
                return false; // not the same -> not equal
            }

            // numbers in Java compare as a class + bit representation
            if (arg1 instanceof Number) {
                return arg1.equals(arg2);
            }

            // arrays
            if (arg1 instanceof IntExpArray) {
                return equalArrays((IntExpArray) arg1, (IntExpArray) arg2);
            }

            if (arg1 instanceof FloatExpArray) {
                return equalArrays((FloatExpArray) arg1, (FloatExpArray) arg2);
            }

            return false;
        }

        static boolean equalArrays(FloatExpArray arg1, FloatExpArray arg2) {
            return equalArrays(arg1.data(), arg2.data());
        }

        static boolean equalArrays(IntExpArray arg1, IntExpArray arg2) {
            return equalArrays(arg1.data(), arg2.data());
        }

        static boolean equalArrays(Object[] arg1, Object[] arg2) {
            int size;
            if ((size = arg1.length) != arg2.length) {
                return false;
            }

            for (int i = 0; i < size; i++) {
                if (!equalArgs(arg1[i], arg2[i])) {
                    return false;
                }
            }

            return true;
        }

        /**
         *
         */
        public ExpressionKeyImpl(Class clazz, Object[] args) {
            _clazz = clazz;
            _args = args;
        }

        @Override
        public Object[] args() {
            return _args;
        }

        @Override
        public Class clazz() {
            return _clazz;
        }

        // Should use more of the objects' equals() methods
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ExpressionKey)) {
                return false;
            }

            ExpressionKey key = (ExpressionKey) o;

            // compare classes
            if (_clazz != key.clazz()) {
                return false;
            }

            // compare argumens
            if (_args.length != key.args().length) {
                return false;
            }

            for (int i = 0; i < _args.length; i++) {
                if (!equalArgs(_args[i], key.args()[i])) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            return _clazz.hashCode() + _args.length;// ???
        }

        /**
         * Returns a String representation of this object.
         *
         * @return a String representation of this object.
         */
        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append("class: ").append(_clazz.getName()).append(", args:(");
            for (int i = 0; i < _args.length; i++) {
                if (i != 0) {
                    s.append(",");
                }
                s.append(_args[i]);
            }
            s.append(")");
            return s.toString();
        }
    } // ~ExpressionKeyImpl

    /**
     * Undo Class for UndoExpressionFactory.
     */
    static class UndoExpressionFactory extends UndoImpl {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoExpressionFactory();
            }

        };

        private Hashtable _expressions;

        static UndoExpressionFactory getUndo() {
            return (UndoExpressionFactory) _factory.getElement();
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

        @Override
        public void undo() {
            ExpressionFactoryImpl expFactory = (ExpressionFactoryImpl) undoable();
            expFactory._expressions = _expressions;
            super.undo();
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            ExpressionFactoryImpl expFactory = (ExpressionFactoryImpl) u;
            _expressions = (Hashtable) expFactory._expressions.clone();
        }

    } // ~UndoExpressionFactory

    /**
     * Cached expressions.
     */
    private Hashtable _expressions = new Hashtable();

    /**
     * Use cache to find already created expression.
     */
    private boolean _getFromCache = false;

    /**
     * Use cache to store newly created expression.
     */
    private boolean _putInCache = false;

    /**
     * Use cache to find and store the expression.
     */
    private boolean _useCache = false;

    /**
     * Returns a constructor with the given parameter types for a given parameter values.
     */
    static Class[] args2types(Object[] args) {
        int size = args.length;
        Class[] types = new Class[size];
        for (int i = 0; i < size; i++) {
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
            Constructor constr = c.getConstructor(types);
            constr.setAccessible(true); // to create not public implementations
            return (Expression) constr.newInstance(args);
        } catch (Exception e) {
            String msg = "Error creating expression: " + e.getClass().getName() + ": " + e.getMessage() + ": " + c
                .getName();

            Constrainer.abort(msg, e);
            return null;
        }
    }

    @Override
    public Undo createUndo() {
        return UndoExpressionFactory.getUndo();
    }

    /**
     * Returns the cached expression for a given key. If there is no cached expression returns null.
     */
    Expression findExpression(ExpressionKey key) {
        return (Expression) _expressions.get(key);
    }

    @Override
    public Expression getExpression(Class clazz, Object[] args) {
        return getExpression(clazz, args, args2types(args));
    }

    @Override
    public Expression getExpression(Class clazz, Object[] args, Class[] types) {
        ExpressionKey key = (_getFromCache || _putInCache ? new ExpressionKeyImpl(clazz, args) : null);
        Expression exp = (_getFromCache ? findExpression(key) : null);

        if (exp == null) {
            exp = createExpression(clazz, args, types);
            // System.out.println("Creating new expression: " + exp.name());
            if (_putInCache) {
                addUndo();
                _expressions.put(key, exp);
            }
        }

        return exp;
    }

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        Enumeration e = _expressions.keys();
        while (e.hasMoreElements()) {
            ExpressionKey key = (ExpressionKey) e.nextElement();
            Expression exp = (Expression) _expressions.get(key);
            s.append(exp.getClass().getName()).append(", ").append(System.identityHashCode(exp)).append(", ");
            for (int i = 0; i < key.args().length; i++) {
                if (i != 0) {
                    s.append(", ");
                }
                Object o = key.args()[i];
                if (o instanceof Number) {
                    s.append(o.getClass().getName()).append(", ").append(o);
                } else {
                    s.append(o.getClass().getName()).append(", ").append(System.identityHashCode(o));
                }
            }
            s.append('\n');
        }
        return s.toString();
    }

    @Override
    public boolean useCache() {
        return _useCache;
    }

    @Override
    public void useCache(boolean flag) {
        _useCache = flag;
        _getFromCache = flag;
        _putInCache = flag;
    }

} // ~ExpressionFactoryImpl
