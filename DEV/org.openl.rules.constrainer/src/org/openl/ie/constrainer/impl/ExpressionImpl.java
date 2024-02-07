package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Expression;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////

/**
 * A generic implementation of the Expression interface.
 */
public abstract class ExpressionImpl extends SubjectImpl implements Expression {
    protected static final Class[] ARGS_Int = {Constrainer.class, int.class};

    protected static final Class[] ARGS_IntExpInt = {IntExp.class, int.class};
    protected static final Class[] ARGS_IntExpIntExp = {IntExp.class, IntExp.class};

    protected static final Class[] ARGS_IntBoolExp2 = {IntBoolExp.class, IntBoolExp.class};

    protected static Expression getExpression(Constrainer c, Class clazz, Object[] args) {
        return c.expressionFactory().getExpression(clazz, args);
    }

    protected static Expression getExpression(Constrainer c, Class clazz, Object[] args, Class[] types) {
        return c.expressionFactory().getExpression(clazz, args, types);
    }

    public ExpressionImpl(Constrainer constrainer, String name) {
        super(constrainer, name);
    }

    protected final Expression getExpression(Class clazz, Object[] args) {
        return constrainer().expressionFactory().getExpression(clazz, args);
    }

    protected final Expression getExpression(Class clazz, Object[] args, Class[] types) {
        return constrainer().expressionFactory().getExpression(clazz, args, types);
    }

    protected final IntBoolExp getIntBoolExp(Class clazz, IntBoolExp exp1, IntBoolExp exp2) {
        return (IntBoolExp) getExpression(clazz, new Object[]{exp1, exp2}, ARGS_IntBoolExp2);
    }

    protected final IntBoolExp getIntBoolExp(Class clazz, IntExp exp, int value) {
        return (IntBoolExp) getExpression(clazz, new Object[]{exp, value}, ARGS_IntExpInt);
    }

    protected final IntBoolExp getIntBoolExp(Class clazz, IntExp exp1, IntExp exp2) {
        return (IntBoolExp) getExpression(clazz, new Object[]{exp1, exp2}, ARGS_IntExpIntExp);
    }

    protected final IntExp getIntExp(Class clazz, int value) {
        return (IntExp) getExpression(clazz, new Object[]{constrainer(), value}, ARGS_Int);
    }

    protected final IntExp getIntExp(Class clazz, IntExp exp, int value) {
        return (IntExp) getExpression(clazz, new Object[]{exp, value}, ARGS_IntExpInt);
    }

    protected final IntExp getIntExp(Class clazz, IntExp exp1, IntExp exp2) {
        return (IntExp) getExpression(clazz, new Object[]{exp1, exp2}, ARGS_IntExpIntExp);
    }

    @Override
    public void name(String name) {
        symbolicName(name);
    }

} // ~ExpressionImpl
